from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "tools" / "validate_contracts.py"
SPEC = importlib.util.spec_from_file_location("validate_contracts", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


class ContractValidationTests(unittest.TestCase):
    def test_repository_contracts_validate(self) -> None:
        file_count, operation_count, example_count = validator.validate_all()
        self.assertGreaterEqual(file_count, 10)
        self.assertGreaterEqual(operation_count, 30)
        self.assertGreaterEqual(example_count, 7)

    def test_money_rejects_json_number(self) -> None:
        schema_path = validator.ROOT / "json-schema/common/money.v1.schema.json"
        schema = validator.load_json(schema_path)
        with self.assertRaises(validator.ContractValidationError):
            validator.validate_instance({"amount": 125000.00, "currency": "KZT"}, schema, schema_path, schema)

    def test_money_rejects_excess_scale(self) -> None:
        schema_path = validator.ROOT / "json-schema/common/money.v1.schema.json"
        schema = validator.load_json(schema_path)
        with self.assertRaises(validator.ContractValidationError):
            validator.validate_instance({"amount": "1.001", "currency": "KZT"}, schema, schema_path, schema)

    def test_phone_shaped_sample_is_rejected(self) -> None:
        with self.assertRaises(validator.ContractValidationError):
            validator.check_no_phone_shaped_values({"alias": "+7 701 123 45 67"}, "negative test")

    def test_disallowed_score_field_is_rejected(self) -> None:
        with self.assertRaises(validator.ContractValidationError):
            validator.check_disallowed_fields({"properties": {"riskScore": {"type": "number"}}}, "negative test")


if __name__ == "__main__":
    unittest.main()
