const CURRENCY_SYMBOLS: Readonly<Record<string, string>> = Object.freeze({
  KZT: '₸',
  USD: '$',
  EUR: '€',
});

const MONEY_PATTERN = /^-?(?:0|[1-9]\d*)(?:\.\d{1,2})?$/;

export function parseMinorUnits(value: string): bigint {
  if (!MONEY_PATTERN.test(value)) {
    throw new TypeError(`Invalid monetary amount: ${value}`);
  }

  const negative = value.startsWith('-');
  const unsigned = negative ? value.slice(1) : value;
  const [major = '0', fraction = ''] = unsigned.split('.');
  const minor = BigInt(major) * 100n + BigInt(fraction.padEnd(2, '0'));

  return negative ? -minor : minor;
}

export function formatMoney(value: string, currency: string, locale = 'ru-KZ'): string {
  const minor = parseMinorUnits(value);
  const negative = minor < 0n;
  const absolute = negative ? -minor : minor;
  const major = absolute / 100n;
  const cents = (absolute % 100n).toString().padStart(2, '0');
  const grouped = new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(major);
  const symbol = CURRENCY_SYMBOLS[currency] ?? currency;

  return `${negative ? '−' : ''}${grouped},${cents}\u00A0${symbol}`;
}

export function formatSignedMoney(value: string, currency: string): string {
  const amount = formatMoney(value, currency);
  return parseMinorUnits(value) > 0n ? `+${amount}` : amount;
}
