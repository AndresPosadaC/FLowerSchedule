/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packingoperation;

import java.util.*;

/**
 *
 * @author andresposadac
 */
public class Demand {

    private final String variety, boxSize, poType;
    private final int poId, keyProduct, length, openness, poStemsQty, pty1, pty2;
    private final double poBoxQty;
    public int stemsBunch, bunchBox, stemsBox;

    public Demand(int keyProduct, String variety, int length, int openness,
            String boxSize, String poType, int poId, double poBoxQty,
            int stemsBunch, int bunchBox, int stemsBox, int poStemsQty, int pty1, int pty2) {
        this.keyProduct = keyProduct;
        this.variety = variety;
        this.length = length;
        this.openness = openness;
        this.boxSize = boxSize;
        this.poType = poType;
        this.poId = poId;
        this.poBoxQty = poBoxQty;
        this.stemsBunch = stemsBunch;
        this.bunchBox = bunchBox;
        this.stemsBox = stemsBox;
        this.poStemsQty = poStemsQty;
        this.pty1 = pty1;
        this.pty2 = pty2;
    }

    @Override
    public String toString() {
        return keyProduct + "\t" + variety + "\t" + length + "\t" + openness
                + "\t" + boxSize + "\t" + poType + "\t" + poId + "\t" + poBoxQty
                + "\t" + stemsBunch + "\t" + bunchBox + "\t" + stemsBox + "\t"
                + poStemsQty + "\t" + pty1 + "\t" + pty2;
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

    public String getBoxSize() {
        return boxSize;
    }

    public int getPoId() {
        return poId;
    }

    public double getPoBoxQty() {
        return poBoxQty;
    }

    public int getStemsBunch() {
        return stemsBox;
    }

    public int getBunchBox() {
        return bunchBox;
    }

    public int getStemsBox() {
        return stemsBox;
    }

    public int getPoStemsQty() {
        return poStemsQty;
    }

    public int getPty1() {
        return pty1;
    }

    public int getPty2() {
        return pty2;
    }

}
