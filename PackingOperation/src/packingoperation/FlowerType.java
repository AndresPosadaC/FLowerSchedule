/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packingoperation;

/**
 *
 * @author andresposadac
 */
public class FlowerType implements Comparable<FlowerType> {

    private final String variety, longKey;
    private final int length, openness;

    public FlowerType(String variety, int length, int openness, String longKey) {
        this.variety = variety;
        this.length = length;
        this.openness = openness;
        this.longKey = variety + length + openness;
    }

    @Override
    public String toString() {
        return variety + "\t" + length + "\t" + openness + "\t" + longKey;
    }

    public String getVariety() {
        return variety;
    }

    public int getLength() {
        return length;
    }

    public int getOpenness() {
        return openness;
    }

    @Override
    public int compareTo(FlowerType product) {

        if(variety.compareTo(product.variety) == 0 &
                length == product.length &
                openness == product.openness ){
            int value = 0;
            return value;
        }
        return longKey.compareTo(product.longKey);
    }
    
}
