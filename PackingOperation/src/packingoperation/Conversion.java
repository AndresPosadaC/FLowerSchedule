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
public class Conversion implements Comparable<Conversion> {

    private final String product, boxSize, longKeyP;
    private final int length, stemBunch, bunchBox, stemBox;

    public Conversion(String product, String boxSize, int length,
            int stemBunch, int bunchBox, int stemBox, String longKeyP) {
        this.product = product;
        this.boxSize = boxSize;
        this.length = length;
        this.stemBunch = stemBunch;
        this.bunchBox = bunchBox;
        this.stemBox = stemBox;
        this.longKeyP = product + boxSize + length;
    }

        @Override
    public String toString() {
        return stemBunch + "\t" + bunchBox + "\t" + stemBox;
    }
    
    public String getProduct() {
        return product;
    }

    public String getBoxSize() {
        return boxSize;
    }

    public int getLength() {
        return length;
    }

    public int getStemBunch() {
        return stemBunch;
    }

    public int getBunchBox() {
        return bunchBox;
    }

    public int getStemBox() {
        return stemBox;
    }

    public String getLongKeyP() {
        return longKeyP;
    }

    @Override
    public int compareTo(Conversion product) {
        return longKeyP.compareTo(product.longKeyP);
    }
}
