package net.torvald.terrarum.itemproperties

import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.isBlock
import net.torvald.terrarum.gameitems.isWall
import net.torvald.terrarum.utils.forEachSiblings
import net.torvald.terrarum.utils.forEachSiblingsIndexed

/**
 * Created by minjaesong on 2022-06-24.
 */
class CraftingCodex {

    /**
     * Key: final product of the given recipes. Equal to the recipe.product
     * Value: the recipes
     */
    @Transient internal val props = HashMap<ItemID, ArrayList<CraftingRecipe>>() // the key ItemID and value.product must be equal

    @Transient internal val smeltingProps = HashMap<String, SmeltingRecipe>()

    fun addRecipe(recipe: CraftingRecipe) {
        val product = recipe.product
        if (props.containsKey(product)) {
            props[product]?.add(recipe)
        }
        else {
            props[product] = arrayListOf(recipe)
        }
    }

    fun addFromJson(json: JsonValue, moduleName: String, fileName:String) {

        if (moduleName.filter { it.code in 33..127 } .length < 5)
            throw IllegalStateException("Invalid module name: ${moduleName}")

        json.forEachSiblings { itemName, details ->
            val workbenchStr = details["workbench"].asString()
            val recipes = ArrayList<CraftingRecipe>()

            details["ingredients"].forEachSiblingsIndexed { ingredientIndex, _, ingredientRecord ->
                var moq = -1L
                val qtys = ArrayList<Long>()
                val itemsStr = ArrayList<String>()
                ingredientRecord.forEachIndexed { i, elem ->
                    if (i == 0) {
                        moq = elem.asLong()
                    }
                    else {
                        if (i % 2 == 1)
                            qtys.add(elem.asLong())
                        else
                            itemsStr.add(elem.asString())
                    }
                }

                // sanity check
                if (moq !in 1..1000)
                    throw IllegalStateException("Recipe #${ingredientIndex+1} for item '$itemName' has invalid moq of ${moq}")
                else if (qtys.size != itemsStr.size)
                    throw IllegalStateException("Mismatched item name and count for recipe #${ingredientIndex+1} for item '$itemName'")

                val ingredients = ArrayList<CraftingIngredients>()
                itemsStr.forEachIndexed { i, itemStr ->
                    ingredients.add(CraftingIngredients(
                            if (itemStr.startsWith("$"))
                                itemStr.substring(1).replace('$',',')
                            else
                                itemStr
                            ,
                            if (itemStr.startsWith("$"))
                                CraftingItemKeyMode.TAG
                            else
                                CraftingItemKeyMode.VERBATIM
                            ,
                            qtys[i]
                    ))
                }
                recipes.add(CraftingRecipe(workbenchStr, ingredients.toTypedArray(), moq, itemName,  "$moduleName/$fileName"))
            }

            // register to the main props
            if (props[itemName] == null) props[itemName] = ArrayList()
            props[itemName]!!.addAll(recipes)
        }
    }

    fun addSmeltingFromJson(json: JsonValue, moduleName: String, fileName:String) {

        if (moduleName.filter { it.code in 33..127 } .length < 5)
            throw IllegalStateException("Invalid module name: ${moduleName}")

        json.forEachSiblings { key0, details ->
            val key = key0.split('+').map { it.trim() }.sorted().joinToString("+")
            val difficulty = details["difficulty"].asFloat()
            val recipes = ArrayList<SmeltingRecipe>()
            val moq = details["product"][0].asLong()
            val product = details["product"][1].asString()
            // sanity check
            if (moq !in 1..1000) {
                throw IllegalStateException("Smelting Recipe for item '$product' has invalid moq of ${moq}")
            }

            // register to the main props
            smeltingProps[key] = SmeltingRecipe(difficulty, key.split('+').toTypedArray(), moq, product, "$moduleName/$fileName")
        }
    }

    /**
     * Returns list of all possible recipes for the item; null if there is no recipe
     *
     * Even if the props for the specified item is happens to exist but has no element (usually caused by bad mod behaviour),
     * this function is guaranteed to return null.
     */
    fun getRecipesFor(itemID: ItemID): List<CraftingRecipe>? = props[itemID]?.toList()?.let {
        return it.ifEmpty { null }
    }

    private fun getCombinations0(data: List<List<String>>): List<List<String>> {
        if (data.isEmpty()) {
            return listOf(emptyList())
        }
        val first = data.first()
        val rest = data.subList(1, data.size)
        val combinations = mutableListOf<List<String>>()
        for (sublist in getCombinations0(rest)) {
            for (item in first) {
                combinations.add(listOf(item) + sublist)
            }
        }
        return combinations
    }

    private fun getCombinations(data: List<List<String>>): List<String> {
        val r = mutableListOf<String>()
        val l = getCombinations0(data)
        l.forEach {
            r.add(it.sorted().joinToString("+"))
        }
        return r
    }


    /**
     * @return Null if:
     * - at least one of the vararg contains null
     * - a smelting product of given items does not exist
     * - Otherwise, a smelting product of the given items are returned
     */
    fun getSmeltingProductOf(vararg item: ItemID?): SmeltingRecipe? {
        if (item.contains(null)) return null

        val keysPossible0: List<List<String>> =
            (item.map { listOf(it!!) + (ItemCodex[it]?.tags?.toList()?.map { "\$" + it } ?: emptyList()) })
        val keysPossible = getCombinations(keysPossible0)

        // iterate through all the combinations of keys
        var found: SmeltingRecipe? = null
        var i = 0
        while (found == null && i < keysPossible.size) {
            found = smeltingProps[keysPossible[i]]
            i += 1
        }

        return found
    }

    /**
     * Returns list of items that uses the given `item`.
     *
     * @return list of itemIDs and corresponding recipes
     */
    fun getCraftableRecipesUsingTheseItems(item: ItemID): List<CraftingRecipe> {
        return props.values.flatten().filter { recipe ->
            recipe.ingredients.any { ingredient ->
                when (ingredient.keyMode) {
                    CraftingItemKeyMode.TAG -> if (item.isBlock() || item.isWall())
                        Terrarum.blockCodex[item].hasTag(ingredient.key)
                    else
                        Terrarum.itemCodex[item]!!.hasTag(ingredient.key)

                    CraftingItemKeyMode.VERBATIM -> (item == ingredient.key)
                }
            }
        }
    }


    /**
     * @return list of itemIDs and corresponding recipes
     */
    fun getRecipesForIngredients(ingredients: List<Pair<ItemID, Long>>): List<CraftingRecipe> {
        TODO()
    }




    data class SmeltingRecipe(val difficulty: Float, val ingredients: Array<ItemID>, val moq: Long, val item: ItemID, val addedBy: String)
    data class CraftingRecipe(val workbench: String, val ingredients: Array<CraftingIngredients>, val moq: Long, val product: ItemID, val addedBy: String)
    data class CraftingIngredients(val key: String, val keyMode: CraftingItemKeyMode, val qty: Long) {
        override fun toString() = "$qty ${if (keyMode == CraftingItemKeyMode.TAG) "\$$key" else "$key"}"
    }
    enum class CraftingItemKeyMode { VERBATIM, TAG }
}

