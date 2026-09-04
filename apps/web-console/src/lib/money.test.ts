import { describe, expect, it } from 'vitest';

import { formatMoney, parseMinorUnits } from './money';

describe('money formatting', () => {
  it('formats KZT decimal strings without losing fractional precision', () => {
    const value = formatMoney('50000.25', 'KZT');

    expect(value).toContain('50');
    expect(value).toContain('000');
    expect(value).toContain('25');
    expect(value).toContain('₸');
  });

  it('converts a decimal string to minor units without floating-point arithmetic', () => {
    expect(parseMinorUnits('9007199254740991.99')).toBe(900719925474099199n);
    expect(parseMinorUnits('-12.3')).toBe(-1230n);
  });

  it('rejects malformed monetary values', () => {
    expect(() => parseMinorUnits('12.345')).toThrow(/monetary amount/i);
    expect(() => parseMinorUnits('NaN')).toThrow(/monetary amount/i);
  });
});
