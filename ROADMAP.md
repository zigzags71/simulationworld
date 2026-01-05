# ROADMAP

## Now (v0.4.5)
- Action gating via affordances (cooldown-aware broadcasts, no "dumb" actions)
- Broadcast cost only applied when a signal is emitted
- Social Credit v0: leader-weighted signals and rewards for helpful broadcasts

## Next
- Patterns/Macros: chainable rules and reusable behavior snippets

## Later
- Social Economy v1: trade food/info for social credit
- Tax direction: hazard raises transfer tax, crowding reduces it (word-of-mouth)
  - Example sketch: `tax = clamp(baseTax + hazardK * hazardNorm - crowdK * crowdNorm, minTax, maxTax)`
