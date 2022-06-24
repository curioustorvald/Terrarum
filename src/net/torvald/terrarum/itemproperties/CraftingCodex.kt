package net.torvald.terrarum.itemproperties

import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.utils.forEachSiblings
import net.torvald.terrarum.utils.forEachSiblingsIndexed

/**
 * Created by minjaesong on 2022-06-24.
 */
class CraftingCodex {

    @Transient private val props = HashMap<ItemID, ArrayList<CraftingRecipe>>()

    fun addFromJson(json: JsonValue, moduleName: String) {

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
                if (moq < 1)
                    throw IllegalStateException("Recipe #${ingredientIndex+1} for item '$itemName' has moq of ${moq}")
                else if (qtys.size != itemsStr.size)
                    throw IllegalStateException("Mismatched item name and count for recipe #${ingredientIndex+1} for item '$itemName'")

                val ingredients = ArrayList<CraftingIngredients>()
                itemsStr.forEachIndexed { i, itemStr ->
                    ingredients.add(CraftingIngredients(
                            if (itemStr.startsWith("$"))
                                itemStr.substring(1)
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
                recipes.add(CraftingRecipe(workbenchStr, ingredients.toTypedArray(), moq, moduleName))
            }

            // register to the main props
            if (props[itemName] == null) props[itemName] = ArrayList()
            props[itemName]!!.addAll(recipes)
        }
    }

    /**
     * Returns list of all possible recipes for the item; null if there is no recipe
     *
     * Even if the props for the specified item is happens to exist but has no element (usually caused by bad mod behaviour),
     * this function is guaranteed to return null.
     */
    fun getRecipesFor(itemID: ItemID): List<CraftingRecipe>? = props[itemID]?.toList()?.let {
        return if (it.isNotEmpty()) it else null
    }

    /**
     * @return list of itemIDs and corresponding recipes
     */
    fun getRecipesUsingTheseItems(items: List<ItemID>): List<Pair<ItemID, CraftingRecipe>> {
        TODO()
    }

    /**
     * @return list of itemIDs and corresponding recipes
     */
    fun getRecipesForIngredients(ingredients: List<Pair<ItemID, Long>>): List<Pair<ItemID, CraftingRecipe>> {
        TODO()
    }




    data class CraftingRecipe(val workbench: String, val ingredients: Array<CraftingIngredients>, val moq: Long, val addedBy: String)
    data class CraftingIngredients(val key: String, val keyMode: CraftingItemKeyMode, val qty: Long)
    enum class CraftingItemKeyMode { VERBATIM, TAG }
}

