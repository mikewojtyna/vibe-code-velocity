# Exercise 1: Spot the vibe

**Module:** M1+2 Why vibe coding doesn't work · **Time:** 20 min · **Form:** pairs · **Start:** tag `ex01/start`

## Situation

A developer generated an empty Spring Boot project with Spring Initializr and ran `/init` in a fresh AI session. The
session had no DDD guidance, no skills and no existing code. Then the developer typed one prompt:

> Create city bike rental system

They committed what came back without changes. The build is green: 15 tests pass.

```bash
git switch -c my-ex01 ex01/start
git show --stat HEAD~1      # what the AI produced, in one commit
./mvnw test                 # green
cat CLAUDE.md               # what the AI says it built
```

The code is in `src/main/java/pl/wojtyna/vibecodevelocity/`, one package per feature: `station`, `bike`, `rider`,
`rental`, `shared`.

## Your task

1. **Business decisions.** Find **at least three business decisions nobody from the business approved**. A decision
   can be a rule, a number, a price, or what happens in an edge case. For each one, write down:
   - the file and line;
   - the decision the code makes;
   - the question that should have gone to the product owner instead.
2. **Bugs.** Find **at least one bug or inconsistency the green tests don't catch**. Look at:
   - the tests, and what they don't try;
   - the comments, and whether the code keeps their promises;
   - `CLAUDE.md`, and whether it describes the code truthfully.
3. **Language.** Find **one business action that goes by different names** across the endpoint, the service and
   the entities. Which name would a rider or an operator use?
4. **Change.** Pick one rule (pricing, ending a rental, station capacity or maintenance). List every class you'd
   have to touch if the business changed it. What else would break?

Tips:
- Read it as a product owner would: what would a rider experience?
- Read it as an operator would: what can the staff *not* do?
- Then read it as a reviewer.

## Done when

You have:
- at least three business decisions, each with its question for the product owner;
- at least one bug;
- one action with several names;
- one change traced through the code.

## Debrief questions

- Which decisions would the product owner have made differently? Which one would hurt first in production, and how?
- Why didn't the green build tell you anything? Who wrote the expected values in the tests?
- The prompt said "city bike rental". Rentals, the fleet, stations, pricing, payments and riders all live in one model
  and one transaction. Where are the boundaries hiding?
- `CLAUDE.md` was generated before any code and then describes the code confidently. How much would you trust it as
  context for the next AI session?
- How would the flow handle the same request: intents, then a spec whose examples answer these questions, then a
  plan that places each rule in a bounded context, all before any code (Exercises 7–9)?

The answer key is the tag `ex01/solution` (`git diff ex01/start ex01/solution`). Look at it only in the debrief.
