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
public class Offer {

    private final String variety, unit;
    private final int keyProduct, length, openness, time, qty;

    public Offer(int keyProduct, String variety, int length, int openness,
            int time, int qty, String unit) {
        this.keyProduct = keyProduct;
        this.variety = variety;
        this.length = length;
        this.openness = openness;
        this.time = time;
        this.qty = qty;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return keyProduct + "\t" + variety + "\t" + length + "\t" + openness + "\t"
                + time + "\t" + qty + "\t" + unit;
    }

    public int getKeyProduct() {
        return keyProduct;
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

    public int getTime() {
        return time;
    }

    public int getQty() {
        return qty;
    }

    public String getUnit() {
        return unit;
    }
}
