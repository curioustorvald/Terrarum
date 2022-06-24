Multiple JSON files can exist under this directory.

### Workbenches

Some items can only be manufactured on certain workbenches. Every workbench has tags assigned and checked against
this property.

Multiple workbenches are separated by commas, and alternative workbenches are separated by semicolons.

### Ingredient Querying

Ingredients are defined as list of records. Multiple records denote multiple alternative recipes, whereas
entries in a record denote multiple ingredients the recipe requires.

Example:

```
"ingredients": [
    [2, 1, "$WOOD", 1, "$ROCK"],
    [20, 1, "ITEM_PLATFORM_BUILDING_KIT"]
]
```

Each entry is interpreted as:

```[moq, count 1, ingredient 1, count 2, ingredient 2, ...]```

- moq: this combination of ingredients creates this amount of crafted item.

For example:

```[2, 1, "$WOOD", 1, "$ROCK"]```

This line is interpreted as: this item requires 1 tagged-as-wood ingredient and 1 tagged-as-rock ingredient,
and returns 2 of crafted items.

```[20, 1, "ITEM_PLATFORM_BUILDING_KIT"]```

This line is interpreted as: this item requires 1 verbatim item "ITEM_PLATFORM_BUILDING_KIT" and returns
20 of crafted items.

Therefore, the single record has at least three elements and always has odd number of them.