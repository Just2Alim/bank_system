#!/usr/bin/env python3
"""Dependency-free structural and example validation for V1 contracts."""

from __future__ import annotations

import argparse
import json
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Any
from urllib.parse import urlparse


ROOT = Path(__file__).resolve().parents[1]
HTTP_METHODS = {"get", "put", "post", "delete", "patch", "options", "head", "trace"}
DISALLOWED_FIELD_NAMES = {
    "pan",
    "primaryaccountnumber",
    "iin",
    "phone",
    "phonenumber",
    "mobilephone",
    "riskscore",
    "fraudscore",
    "modelscore",
    "anomalyscore",
}


class ContractValidationError(ValueError):
    """Raised when a contract or example violates the frozen baseline."""


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ContractValidationError(f"{path}: invalid JSON: {exc}") from exc


def json_files() -> list[Path]:
    return sorted(path for path in ROOT.rglob("*.json") if not path.name.startswith("._"))


def decode_pointer(document: Any, fragment: str, context: str) -> Any:
    current = document
    if fragment in ("", "#"):
        return current
    if not fragment.startswith("#/"):
        raise ContractValidationError(f"{context}: unsupported JSON pointer {fragment!r}")
    for encoded in fragment[2:].split("/"):
        token = encoded.replace("~1", "/").replace("~0", "~")
        try:
            current = current[int(token)] if isinstance(current, list) else current[token]
        except (KeyError, IndexError, ValueError, TypeError) as exc:
            raise ContractValidationError(f"{context}: unresolved pointer {fragment!r}") from exc
    return current


def resolve_ref(ref: str, base_path: Path, root_document: Any) -> tuple[Any, Path, Any]:
    if ref.startswith("http://") or ref.startswith("https://"):
        raise ContractValidationError(f"{base_path}: remote $ref is not allowed: {ref}")
    file_part, separator, fragment_part = ref.partition("#")
    fragment = f"#{fragment_part}" if separator else ""
    if file_part:
        target_path = (base_path.parent / file_part).resolve()
        try:
            target_path.relative_to(ROOT.resolve())
        except ValueError as exc:
            raise ContractValidationError(f"{base_path}: $ref escapes contracts root: {ref}") from exc
        target_document = load_json(target_path)
    else:
        target_path = base_path
        target_document = root_document
    return decode_pointer(target_document, fragment, f"{base_path} -> {ref}"), target_path, target_document


def check_refs(node: Any, base_path: Path, root_document: Any, seen: set[tuple[Path, str]]) -> None:
    if isinstance(node, dict):
        ref = node.get("$ref")
        if isinstance(ref, str):
            key = (base_path.resolve(), ref)
            if key not in seen:
                seen.add(key)
                resolved, target_path, target_document = resolve_ref(ref, base_path, root_document)
                check_refs(resolved, target_path, target_document, seen)
        for value in node.values():
            check_refs(value, base_path, root_document, seen)
    elif isinstance(node, list):
        for value in node:
            check_refs(value, base_path, root_document, seen)


def normalized_field_name(value: str) -> str:
    return re.sub(r"[^a-z0-9]", "", value.lower())


def check_disallowed_fields(node: Any, context: str) -> None:
    if isinstance(node, dict):
        properties = node.get("properties")
        if isinstance(properties, dict):
            for field_name in properties:
                if normalized_field_name(field_name) in DISALLOWED_FIELD_NAMES:
                    raise ContractValidationError(f"{context}: disallowed contract field {field_name!r}")
        for value in node.values():
            check_disallowed_fields(value, context)
    elif isinstance(node, list):
        for value in node:
            check_disallowed_fields(value, context)


def check_no_phone_shaped_values(node: Any, context: str) -> None:
    if isinstance(node, dict):
        for key, value in node.items():
            if normalized_field_name(key) in DISALLOWED_FIELD_NAMES:
                raise ContractValidationError(f"{context}: disallowed example field {key!r}")
            check_no_phone_shaped_values(value, context)
    elif isinstance(node, list):
        for value in node:
            check_no_phone_shaped_values(value, context)
    elif isinstance(node, str):
        compact = re.sub(r"[\s()+-]", "", node)
        if re.fullmatch(r"(?:7|8)\d{10}", compact):
            raise ContractValidationError(f"{context}: phone-shaped sample value is forbidden")


def type_matches(instance: Any, expected: str) -> bool:
    checks = {
        "null": lambda value: value is None,
        "object": lambda value: isinstance(value, dict),
        "array": lambda value: isinstance(value, list),
        "string": lambda value: isinstance(value, str),
        "boolean": lambda value: isinstance(value, bool),
        "integer": lambda value: isinstance(value, int) and not isinstance(value, bool),
        "number": lambda value: isinstance(value, (int, float)) and not isinstance(value, bool),
    }
    if expected not in checks:
        raise ContractValidationError(f"unsupported schema type {expected!r}")
    return checks[expected](instance)


def validate_format(value: str, format_name: str, location: str) -> None:
    if format_name == "date-time":
        try:
            datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError as exc:
            raise ContractValidationError(f"{location}: invalid date-time {value!r}") from exc
    elif format_name == "uri-reference":
        parsed = urlparse(value)
        if not (parsed.scheme or parsed.path):
            raise ContractValidationError(f"{location}: invalid URI reference {value!r}")


def validate_instance(instance: Any, schema: Any, schema_path: Path, root_schema: Any, location: str = "$") -> None:
    if not isinstance(schema, dict):
        raise ContractValidationError(f"{schema_path}: schema at {location} is not an object")

    if "$ref" in schema:
        resolved, target_path, target_root = resolve_ref(schema["$ref"], schema_path, root_schema)
        validate_instance(instance, resolved, target_path, target_root, location)
        return

    if "allOf" in schema:
        for index, branch in enumerate(schema["allOf"]):
            validate_instance(instance, branch, schema_path, root_schema, f"{location}.allOf[{index}]")

    if "oneOf" in schema:
        matches = 0
        errors: list[str] = []
        for branch in schema["oneOf"]:
            try:
                validate_instance(instance, branch, schema_path, root_schema, location)
                matches += 1
            except ContractValidationError as exc:
                errors.append(str(exc))
        if matches != 1:
            raise ContractValidationError(f"{location}: expected exactly one oneOf match, got {matches}; {errors[:2]}")

    if "const" in schema and instance != schema["const"]:
        raise ContractValidationError(f"{location}: expected const {schema['const']!r}, got {instance!r}")
    if "enum" in schema and instance not in schema["enum"]:
        raise ContractValidationError(f"{location}: {instance!r} is not in {schema['enum']!r}")

    expected_types = schema.get("type")
    if expected_types is not None:
        expected_types = expected_types if isinstance(expected_types, list) else [expected_types]
        if not any(type_matches(instance, expected) for expected in expected_types):
            raise ContractValidationError(f"{location}: expected type {expected_types!r}, got {type(instance).__name__}")

    if isinstance(instance, dict):
        required = schema.get("required", [])
        missing = [name for name in required if name not in instance]
        if missing:
            raise ContractValidationError(f"{location}: missing required properties {missing!r}")
        properties = schema.get("properties", {})
        for name, value in instance.items():
            if name in properties:
                validate_instance(value, properties[name], schema_path, root_schema, f"{location}.{name}")
            elif schema.get("additionalProperties") is False:
                raise ContractValidationError(f"{location}: unexpected property {name!r}")
            elif isinstance(schema.get("additionalProperties"), dict):
                validate_instance(value, schema["additionalProperties"], schema_path, root_schema, f"{location}.{name}")

    if isinstance(instance, list):
        if "minItems" in schema and len(instance) < schema["minItems"]:
            raise ContractValidationError(f"{location}: expected at least {schema['minItems']} items")
        if "maxItems" in schema and len(instance) > schema["maxItems"]:
            raise ContractValidationError(f"{location}: expected at most {schema['maxItems']} items")
        if schema.get("uniqueItems"):
            encoded = [json.dumps(value, sort_keys=True) for value in instance]
            if len(encoded) != len(set(encoded)):
                raise ContractValidationError(f"{location}: array items are not unique")
        if "items" in schema:
            for index, value in enumerate(instance):
                validate_instance(value, schema["items"], schema_path, root_schema, f"{location}[{index}]")

    if isinstance(instance, str):
        if "minLength" in schema and len(instance) < schema["minLength"]:
            raise ContractValidationError(f"{location}: string is shorter than {schema['minLength']}")
        if "maxLength" in schema and len(instance) > schema["maxLength"]:
            raise ContractValidationError(f"{location}: string is longer than {schema['maxLength']}")
        if "pattern" in schema and re.fullmatch(schema["pattern"], instance) is None:
            raise ContractValidationError(f"{location}: {instance!r} does not match {schema['pattern']!r}")
        if "format" in schema:
            validate_format(instance, schema["format"], location)

    if isinstance(instance, (int, float)) and not isinstance(instance, bool):
        if "minimum" in schema and instance < schema["minimum"]:
            raise ContractValidationError(f"{location}: {instance} is below minimum {schema['minimum']}")
        if "maximum" in schema and instance > schema["maximum"]:
            raise ContractValidationError(f"{location}: {instance} exceeds maximum {schema['maximum']}")


def operation_parameter_names(operation: dict[str, Any], path_item: dict[str, Any], document: Any, path: Path) -> set[str]:
    names: set[str] = set()
    for parameter in [*path_item.get("parameters", []), *operation.get("parameters", [])]:
        if "$ref" in parameter:
            parameter, _, _ = resolve_ref(parameter["$ref"], path, document)
        name = parameter.get("name")
        if isinstance(name, str):
            names.add(name.lower())
    return names


def validate_openapi(path: Path, document: dict[str, Any], operation_ids: set[str]) -> int:
    if not str(document.get("openapi", "")).startswith("3.1."):
        raise ContractValidationError(f"{path}: expected OpenAPI 3.1.x")
    if not isinstance(document.get("paths"), dict) or not document["paths"]:
        raise ContractValidationError(f"{path}: paths must be a non-empty object")

    operation_count = 0
    for route, path_item in document["paths"].items():
        if not route.startswith("/") or not isinstance(path_item, dict):
            raise ContractValidationError(f"{path}: invalid path item {route!r}")
        for method, operation in path_item.items():
            if method not in HTTP_METHODS:
                continue
            operation_count += 1
            operation_id = operation.get("operationId")
            if not isinstance(operation_id, str) or not operation_id:
                raise ContractValidationError(f"{path}: {method.upper()} {route} lacks operationId")
            if operation_id in operation_ids:
                raise ContractValidationError(f"{path}: duplicate operationId {operation_id!r}")
            operation_ids.add(operation_id)
            if not isinstance(operation.get("responses"), dict) or not operation["responses"]:
                raise ContractValidationError(f"{path}: {operation_id} lacks responses")
            parameter_names = operation_parameter_names(operation, path_item, document, path)
            if "x-correlation-id" not in parameter_names:
                raise ContractValidationError(f"{path}: {operation_id} lacks X-Correlation-ID")
            if method == "post" and not operation.get("x-idempotency-exempt") and "idempotency-key" not in parameter_names:
                raise ContractValidationError(f"{path}: {operation_id} lacks Idempotency-Key")
            if path.name.startswith("gateway-") and method == "post" and "x-csrf-token" not in parameter_names:
                raise ContractValidationError(f"{path}: {operation_id} lacks X-CSRF-Token")
    return operation_count


def validate_all() -> tuple[int, int, int]:
    files = json_files()
    documents = {path: load_json(path) for path in files}
    for path, document in documents.items():
        check_refs(document, path, document, set())
        check_disallowed_fields(document, str(path.relative_to(ROOT)))

    operation_ids: set[str] = set()
    openapi_operations = 0
    for path, document in documents.items():
        if path.name.endswith(".openapi.json"):
            openapi_operations += validate_openapi(path, document, operation_ids)

    manifest = documents[ROOT / "validation_manifest.json"]
    validated_examples = 0
    for entry in manifest.get("examples", []):
        document_path = ROOT / entry["document"]
        schema_path = ROOT / entry["schema"]
        instance = documents[document_path]
        schema = documents[schema_path]
        check_no_phone_shaped_values(instance, entry["document"])
        validate_instance(instance, schema, schema_path, schema)
        validated_examples += 1

    return len(files), openapi_operations, validated_examples


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    try:
        file_count, operation_count, example_count = validate_all()
    except ContractValidationError as exc:
        print(f"CONTRACT VALIDATION FAILED: {exc}", file=sys.stderr)
        return 1
    print(
        f"CONTRACT VALIDATION PASSED: {file_count} JSON files, "
        f"{operation_count} OpenAPI operations, {example_count} schema-bound examples"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
