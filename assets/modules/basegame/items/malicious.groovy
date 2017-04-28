import net.torvald.terrarum.itemproperties.InventoryItem
import net.torvald.terrarum.itemproperties.Material
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/**
 * Created by SKYHi14 on 2017-04-28.
 */

static InventoryItem invoke(int id) {

    return new InventoryItem() {
        @Override
        int getDynamicID() {
            return 0
        }

        @Override
        void setDynamicID(int i) {

        }

        @Override
        int getOriginalID() {
            return 0
        }

        @Override
        boolean isUnique() {
            return false
        }

        @Override
        protected String getOriginalName() {
            return null
        }

        @Override
        double getBaseMass() {
            return 0
        }

        @Override
        void setBaseMass(double v) {

        }

        @Override
        Double getBaseToolSize() {
            return null
        }

        @Override
        void setBaseToolSize(@Nullable Double aDouble) {

        }

        @Override
        String getInventoryCategory() {
            return null
        }

        @Override
        void setInventoryCategory(@NotNull String s) {

        }

        @Override
        boolean getStackable() {
            return false
        }

        @Override
        void setStackable(boolean b) {

        }

        @Override
        boolean isDynamic() {
            return false
        }

        @Override
        Material getMaterial() {
            return null
        }
    }
}