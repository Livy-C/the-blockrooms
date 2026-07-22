package name.blockrooms.util;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemList extends ArrayList<ItemStack> {
    public ItemList(List<ItemStack> stackList){
        super(stackList);
    }
    public ItemList(){

    }
    @Override
    public boolean add(ItemStack itemStack) {
        boolean f = false;
        for(ItemStack itemStack1 : this){
            if(itemStack1.is(itemStack.getItem())){
                f = true;
                itemStack1.setCount(itemStack1.getCount() + itemStack.getCount());
            }
        }
        if(!f){
            return super.add(itemStack);
        } else {
            return f;
        }
    }
}
