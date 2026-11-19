import { getDailyInspiration } from './daily-inspiration';

describe('getDailyInspiration', () => {
  it('returns the same LifeSync quote throughout a local day', () => {
    const morning = getDailyInspiration(new Date(2026, 10, 17, 7));
    const evening = getDailyInspiration(new Date(2026, 10, 17, 22));

    expect(morning).toEqual(evening);
    expect(morning.author).toBe('LifeSync');
  });

  it('uses a different quote each day until all 100 have been used', () => {
    const quotes = Array.from({ length: 100 }, (_, day) =>
      getDailyInspiration(new Date(2026, 0, day + 1)).quote,
    );

    expect(new Set(quotes).size).toBe(100);
    expect(
      getDailyInspiration(new Date(2026, 0, 101)).quote,
    ).toBe(quotes[0]);
  });
});
