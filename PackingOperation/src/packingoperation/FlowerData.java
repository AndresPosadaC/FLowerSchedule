/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packingoperation;

import java.io.*;
import java.util.*;
//import java.util.stream.Collectors;

/**
 *
 * @author andresposadac
 */
public class FlowerData {

    boolean runAll = true;
    public int demFVal, minutesPeriod, peopleRose, peopleCarnation,
            peopleMini, peopleBox, stemArrange, boxArrange;
    private int intLength, totalStems, factorMult, penalty1, penalty2;
    private int periodsInt;
    private String printMapNset;
    private Scanner input;
    public int bigPty = 1000;
    public int breakPoint1, breakPoint2;
    //private final int keyB = 0;
    private int keyD = 0;
    private int keyP = 0;
    public int periodsUsed, stemsBunch, bunchBox, stemsBox, roseBunchCapacity,
            carnationBunchCapacity, miniBunchCapacity, boxCapacity;
    String longKey;
    String productFile, conversionFile, offerFile, demandFile;
    int[][] tPty;
    Set<Demand> flowerDemand = new HashSet<>();
    Set<Offer> offerInflow = new HashSet<>();
    Map<Integer, Set<Offer>> offerInflowReq = new HashMap<>();
    Set<Offer> offerInflowStems = new HashSet<>();
    Map<String, Conversion> mapCv1 = new LinkedHashMap<>(); //Key = enum(elements) by (Product+Length) 
    Map<String, Conversion> mapCv2 = new LinkedHashMap<>(); //Key = enum(elements) by (Product+boxSize+Length) 
    Map<Integer, String> mapBx1 = new LinkedHashMap<>();
    Map<String, Integer> mapBx2 = new LinkedHashMap<>();
    Map<Integer, FlowerType> mapP = new LinkedHashMap<>(); //Key = enum(elements) by Variety+length(1 to 3 for Carn and Mini, 40 to 90 Roses) + longKey
    Map<Integer, String> mapP2 = new LinkedHashMap<>();   //Same Key to identify variety and for Value = Product (Rose, Carnation...) 
    Map<Integer, String> mapPCol = new LinkedHashMap<>();   //Same Key to identify variety and for Value = Color
    Map<Integer, Set<Demand>> mapDem = new LinkedHashMap<>();
    Map<Integer, Set<Demand>> mapPO = new LinkedHashMap<>(); //Key = POid, Values = Set<Demand>
    Map<Integer, Integer> mapPO2 = new LinkedHashMap<>(); //Key = enum(elements), Values = POid
    Map<Integer, Double> mapEarlyTP1 = new TreeMap<>();
    Map<Integer, Integer> mapEarlyTP = new LinkedHashMap<>();
    Map<Integer, Integer> mapPOint = new LinkedHashMap<>();

    void clearMaps() {
        flowerDemand.clear();
        offerInflow.clear();
        offerInflowReq.clear();
        offerInflowStems.clear();
        mapDem.clear();
        mapPO.clear();
        mapPO2.clear();
        mapEarlyTP1.clear();
        mapEarlyTP.clear();
        mapPOint.clear();
    }

    boolean askParameters(boolean runInstances, int instance) throws IOException {
        clearMaps();

        boolean foundFile = false;
        int workHours = 12;
        minutesPeriod = 20;
        stemArrange = 15;
        boxArrange = 20;
        input = new Scanner(System.in);
        runAll = runInstances;

        if (runAll) {
            demFVal = instance;
            System.out.println("Solve for " + demFVal + " clients");
            filesPath();
        } else {
            System.out.print("\nType number of clients ");
            demFVal = input.nextInt();
            filesPath();
        }

        if (getFlowerParameters()) {
            if (getDemand()) {
                if (getOffer()) {
                    foundFile = true;
                }
            }
        }

        if (foundFile) {
            if (!runAll) {
                System.out.println("\n\t*** General Parameters ***");
                System.out.print("Use preset parameters? Y/N ");
                String defVal = input.next();
                if (defVal.equalsIgnoreCase("N")) {
                    System.out.print("Working Hours per day (eg " + workHours + "): ");
                    workHours = input.nextInt();
                    System.out.print("Minutes per period (eg " + minutesPeriod + "): ");
                    minutesPeriod = input.nextInt();
                    System.out.print("Seconds required per person to arrange one Stem (eg " + stemArrange + "): ");
                    stemArrange = input.nextInt();
                    System.out.print("Seconds required per person to arrange one Box (eg " + boxArrange + "): ");
                    boxArrange = input.nextInt();
                }
            }
            double periods = workHours * 60 / minutesPeriod;
            double periodsNslag = periods * 1.25;
            periodsInt = (int) periods;
            periodsUsed = (int) periodsNslag;
            offerReq();
            peopleReq();
            if (runAll == false) {
                System.out.print("\nDo you want to use default parameters? Y/N :\t");
                String defVal = input.next();
                if (defVal.equalsIgnoreCase("N")) {
                    System.out.print("# Workers bunching Roses (eg " + peopleRose + "): ");
                    peopleRose = input.nextInt();
                    System.out.print("# Workers bunching Carnations (eg " + peopleCarnation + "): ");
                    peopleCarnation = input.nextInt();
                    System.out.print("# Workers bunching MiniCarnations (eg " + peopleMini + "): ");
                    peopleMini = input.nextInt();
                    System.out.print("# Workers boxing Boxes (eg " + peopleBox + "): ");
                    peopleBox = input.nextInt();
                }
            }
            breakPoint1 = (int) (periodsInt / 2.5);
            breakPoint2 = (int) (periodsInt / 1.3);
            roseBunchCapacity = (int) (minutesPeriod * 60 / stemArrange) * peopleRose;
            carnationBunchCapacity = (int) (minutesPeriod * 60 / stemArrange) * peopleCarnation;
            miniBunchCapacity = (int) (minutesPeriod * 60 / stemArrange) * peopleMini;
            boxCapacity = (int) (minutesPeriod * 60 / boxArrange) * peopleBox;

            getPtyVal();
        }
        return foundFile;
    } //end method askParameters

    void filesPath() {
        productFile = "/users/andresposadac/Dropbox/LnsFlowersData/flowersData/DF_Products.csv";
        conversionFile = "/users/andresposadac/Dropbox/LnsFlowersData/flowersData/Flowers_Conversion.csv";
        offerFile = "/users/andresposadac/Dropbox/LnsFlowersData/flowersData/DF_Offer.csv";
        demandFile = "/users/andresposadac/Dropbox/LnsFlowersData/flowersData/Demand_" + demFVal + "_PO.csv";
        //productFile = "DF_Products.csv";
        //conversionFile = "Flowers_Conversion.csv";
        //offerFile = "DF_Offer.csv";
        //demandFile = "Demand_" + demFVal + "_PO.csv";
    }

    String getInstNum(int instance) {
        String instNum = demFVal + "_" + instance;
        return instNum;
    }

    int getPOnumber() {
        return mapPO.size();
    }

    int getPeriods() {
        return periodsUsed;
    }

    boolean getFlowerParameters() throws IOException {

        //productFile = "DF_Products.csv";
        //conversionFile = "Flowers_Conversion.csv";
        File fileName = new File(productFile);
        File c_fileName = new File(conversionFile);
        BufferedReader br = null;
        // Skip the first line (Header) in the file
        String line;

        String csvSplitBy = ",";

        try {

            System.out.println("");
            br = new BufferedReader(new FileReader(c_fileName));

            br.readLine();   //reads the header of the file
            //System.out.println(line);

            while ((line = br.readLine()) != null) {
                // use comma as separator to read csv file
                String[] cFlower = line.split(csvSplitBy);

                String productC = cFlower[0];
                String boxSizeC = cFlower[2];
                int stemBunchC = Integer.parseInt(cFlower[3]);
                int bunchBoxC = Integer.parseInt(cFlower[4]);
                int stemBoxC = Integer.parseInt(cFlower[5]);

                switch (cFlower[1]) {
                    case "SELECT":
                        intLength = 3;
                        break;
                    case "FANCY":
                        intLength = 2;
                        break;
                    case "STANDARD":
                        intLength = 1;
                        break;
                    default:
                        intLength = Integer.parseInt(cFlower[1]);
                        break;
                }
                String longKeyP1 = productC + intLength;
                String longKeyP2 = productC + boxSizeC + intLength;
                Conversion flowerConv = new Conversion(productC, boxSizeC,
                        intLength, stemBunchC, bunchBoxC, stemBoxC, longKeyP2);

                //Map with information about conversion numbers
                mapCv1.put(longKeyP1, flowerConv);
                mapCv2.put(longKeyP2, flowerConv);

                switch (boxSizeC) {
                    case "FB":
                        mapBx1.put(0, boxSizeC);
                        mapBx2.put(boxSizeC, 0);
                        break;
                    case "HB":
                        mapBx1.put(1, boxSizeC);
                        mapBx2.put(boxSizeC, 1);
                        break;
                    case "QB":
                        mapBx1.put(2, boxSizeC);
                        mapBx2.put(boxSizeC, 2);
                        break;
                }

            } // end of while loop

            if (mapCv2.isEmpty()) {
                System.out.println("Conversion Map is empty at start");
            } else {
                System.out.print("\nConversion Map was created and contains "
                        + mapCv2.size() + " elements.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + c_fileName.toString());
            return false;
        } catch (IOException e) {
            System.out.println("Unable to read file: " + c_fileName.toString());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println("Unable to read file: " + c_fileName.toString());
                    return false;
                }
            }
        } // end try catch reading file conversion

        try {
            System.out.println("");
            br = new BufferedReader(new FileReader(fileName));

            br.readLine();   //reads the header of the file
            //System.out.println(line);

            while ((line = br.readLine()) != null) {
                // use comma as separator to read csv file
                String[] flower = line.split(csvSplitBy);
                String varP = flower[0];
                String prodP = flower[1];
                String colorP = flower[2];

                switch (prodP) {
                    case "Carnation":
                        for (int fLength = 1; fLength < 4; fLength++) {
                            for (int fOpenness = 1; fOpenness < 6; fOpenness++) {
                                longKey = varP + fLength + fOpenness;
                                FlowerType productDescription = new FlowerType(varP, fLength,
                                        fOpenness, longKey);
                                mapP.put(keyD, productDescription);  // Mapping the product
                                mapP2.put(keyD, prodP);     //Mapping key + Rose, Carnation or MiniC.
                                mapPCol.put(keyD, colorP);   //Mapping key + Red, Yellow...
                                keyD++;   //Increments the key by 1
                                //String c = mapPCol.get(colorP);
                                //generateOffer(varP, fLength, fOpenness);
                            }
                        }
                        break;
                    case "Minicarnation":
                        for (int fLength = 1; fLength < 4; fLength++) {
                            for (int fOpenness = 1; fOpenness < 6; fOpenness++) {
                                longKey = varP + fLength + fOpenness;
                                FlowerType productDescription = new FlowerType(varP, fLength,
                                        fOpenness, longKey);
                                mapP.put(keyD, productDescription);  // Mapping the product
                                mapP2.put(keyD, prodP);
                                mapPCol.put(keyD, colorP);
                                keyD++;   //Increments the key by 1
                                //generateOffer(varP, fLength, fOpenness);
                            }
                        }
                        break;
                    case "Rose":
                        for (int fLength = 40; fLength < 100; fLength += 10) {
                            for (int fOpenness = 1; fOpenness < 6; fOpenness++) {
                                longKey = varP + fLength + fOpenness;
                                FlowerType productDescription = new FlowerType(varP, fLength,
                                        fOpenness, longKey);
                                mapP.put(keyD, productDescription);  // Mapping the product
                                mapP2.put(keyD, prodP);
                                mapPCol.put(keyD, colorP);
                                keyD++;   //Increments the key by 1
                                //generateOffer(varP, fLength, fOpenness);
                            }
                        }
                        break;
                }

            } // end of while loop reading all products available

            if (mapP.isEmpty()) {
                System.out.println("Product Map is empty at start");
            } else {
                System.out.print("Product Map was created along with mapP2 "
                        + "(Product) and mapPCol (Color) \n and contains "
                        + mapP.size() + " elements.\n");
                //generateDemand(demFVal);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName.toString());
            return false;
        } catch (IOException e) {
            System.out.println("Unable to read file: " + fileName.toString());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println("Unable to read file: " + fileName.toString());
                    return false;
                }
            }
        } // end try catch reading file products
        return true;
    }  //end of getProduct method

    boolean getOffer() throws IOException {

        //offerFile = "DF_Offer.csv";
        File o_fileName = new File(offerFile);
        BufferedReader br = null;
        String line;
        String csvSplitBy = ",";

        //Offer
        try {
            br = new BufferedReader(new FileReader(o_fileName));
            br.readLine();       //reads the header of the file

            while ((line = br.readLine()) != null) {
                // use comma as separator to read csv file
                String[] inflow = line.split(csvSplitBy);
                String varFwr = inflow[0];
                String lengthOffer = inflow[1];
                String oppennesOffer = inflow[2];
                int opennessFwr = Integer.parseInt(oppennesOffer);
                String unitFwr = inflow[3];
                int inflowFwr = Integer.parseInt(inflow[4]);
                int periodInflow = (int) (Integer.parseInt(inflow[5]) / minutesPeriod);

                //To make length an integer
                switch (inflow[1]) {
                    case "SELECT":
                        intLength = 3;
                        break;
                    case "FANCY":
                        intLength = 2;
                        break;
                    case "STANDARD":
                        intLength = 1;
                        break;
                    default:
                        intLength = Integer.parseInt(lengthOffer);
                        break;
                }

                longKey = varFwr + intLength + oppennesOffer;
                FlowerType offerID = new FlowerType(varFwr, intLength,
                        opennessFwr, longKey);

                for (Map.Entry<Integer, FlowerType> entry : mapP.entrySet()) {
                    int key = entry.getKey();
                    int offerInStems = 0;
                    String unitTransfomed = "toStems";
                    FlowerType value = entry.getValue();
                    if (offerID.compareTo(value) == 0) {   //they are equal FlowerType

                        Offer offerDF = new Offer(key, varFwr, intLength,
                                opennessFwr, periodInflow, inflowFwr, unitFwr);

                        offerInflow.add(offerDF);

                        //Use Conversion map to create offerInflowStems form offerInflow.
                        for (Map.Entry<String, Conversion> convObj : mapCv2.entrySet()) {
                            Conversion valueConv = convObj.getValue();
                            if (valueConv.getProduct().equalsIgnoreCase(mapP2.get(key))
                                    && valueConv.getLength() == offerDF.getLength()) {
                                switch (offerDF.getUnit()) {
                                    case "Stem":
                                        offerInStems = offerDF.getQty();
                                        break;
                                    case "Bunch":
                                        offerInStems = offerDF.getQty() * valueConv.getStemBunch();
                                        unitFwr = unitTransfomed;
                                        break;
                                    default:  //Box (FB, HB, QB)
                                        offerInStems = offerDF.getQty() * valueConv.getStemBox();
                                        unitFwr = unitTransfomed;
                                        break;
                                }
                            }
                        }

                        Offer offerDFstems = new Offer(key, varFwr, intLength,
                                opennessFwr, periodInflow, offerInStems, unitFwr);

                        offerInflowStems.add(offerDFstems);

                        break;
                    }
                }

            } // end of while loop

            if (offerInflow.isEmpty()) {
                System.out.println("\nOffer Set is empty at start");
            } else {
                System.out.print("\nOffer Set was created and contains "
                        + offerInflow.size() + " elements. \n");
            }
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + o_fileName.toString());
            return false;
        } catch (IOException e) {
            System.out.println("Unable to read file: " + o_fileName.toString());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println("Unable to read file: " + o_fileName.toString());
                    return false;
                }
            }
        } // end try catch reading file offer  */
    } //end of getOffer method

    boolean getDemand() throws IOException {

        //demandFile = "/users/andresposadac/Dropbox/LnsFlowersData/flowersData/Demand_" + demFVal + "_PO.csv";
        //demandFile = "Demand_" + demFVal + "_PO.csv";
        File fileName = new File(demandFile);
        BufferedReader br = null;
        String line;
        String csvSplitBy = ",";
        String compKey;    //product + boxType + length   eg: RoseFB60

        //Demand
        try {
            System.out.println("");
            br = new BufferedReader(new FileReader(fileName));
            br.readLine();    //reads the header of the file

            double percentP = 1.0;

            while ((line = br.readLine()) != null) {
                // use comma as separator to read csv file
                String[] purchase = line.split(csvSplitBy);
                String varFwr = purchase[0];
                String lengthFwr = purchase[1];
                int opennessFwr = Integer.parseInt(purchase[2]);
                String boxTyp = purchase[3];
                String orderTyp = purchase[4];
                int poNum = Integer.parseInt(purchase[5]);
                double boxNum = Integer.parseInt(purchase[6]);

                switch (lengthFwr) {
                    case "SELECT":
                        intLength = 3;
                        break;
                    case "FANCY":
                        intLength = 2;
                        break;
                    case "STANDARD":
                        intLength = 1;
                        break;
                    default:
                        intLength = Integer.parseInt(lengthFwr);
                        break;
                }

                double boxNum2 = boxNum * percentP;
                boolean assortedReq = false;

                if ("ASRO".equals(varFwr) || "ASCA".equals(varFwr)
                        || "ASMC".equals(varFwr)) {
                    varFwr = assortedDist(purchase, varFwr);
                    assortedReq = true;
                }

                longKey = varFwr + intLength + purchase[2];
                FlowerType demandID = new FlowerType(varFwr, intLength,
                        opennessFwr, longKey);

                if (assortedReq) {
                    stemsBunch = Integer.parseInt(purchase[8]);
                    bunchBox = Integer.parseInt(purchase[9]);
                    stemsBox = stemsBunch * bunchBox;
                    totalStems = (int) (stemsBox * boxNum);
                } else {

                    for (Map.Entry<Integer, FlowerType> entry : mapP.entrySet()) {
                        int key = entry.getKey();
                        FlowerType value = entry.getValue();
                        if (demandID.compareTo(value) == 0) {   // Comparing FlowerType
                            compKey = mapP2.get(key) + boxTyp + intLength;
                            for (Map.Entry<String, Conversion> entry2 : mapCv2.entrySet()) {
                                String value2 = entry2.getValue().getLongKeyP();
                                if (compKey.compareTo(value2) == 0) {
                                    stemsBunch = entry2.getValue().getStemBunch();
                                    bunchBox = entry2.getValue().getBunchBox();
                                    stemsBox = entry2.getValue().getStemBox();
                                    totalStems = (int) (boxNum2 * stemsBunch * bunchBox);
                                    break; // conversion mapCv2 loop
                                }

                            }
                            break; //mapP loop
                        }
                    }
                }

                //System.out.println(poNum + " :> " + boxNum2 + " = "+boxNum + "* " + percentP);
                //Used to calculate the penalty
                if (totalStems > 1000) {
                    factorMult = 20;
                } else if (totalStems > 400) {
                    factorMult = 15;
                } else if (totalStems > 250) {
                    factorMult = 10;
                } else {
                    factorMult = 5;
                }

                switch (orderTyp) {
                    case "F":
                        penalty1 = factorMult * 3;
                        penalty2 = factorMult * 5;
                        break;
                    case "R":
                        penalty1 = factorMult * 2;
                        penalty2 = factorMult * 4;
                        break;
                    case "M":
                        penalty1 = factorMult * 1;
                        penalty2 = factorMult * 3;
                        break;
                }

                for (Map.Entry<Integer, FlowerType> entry : mapP.entrySet()) {
                    int key = entry.getKey(); // variety + length + openness
                    FlowerType value = entry.getValue();

                    if (demandID.compareTo(value) == 0) {  //they are equal FlowerType

                        Demand demandKey = new Demand(key, varFwr, intLength,
                                opennessFwr, boxTyp, orderTyp, poNum, boxNum2,
                                stemsBunch, bunchBox, stemsBox, totalStems,
                                penalty1, penalty2);

                        flowerDemand.add(demandKey);

                        //Group puerchase orders by variety then poID
                        Set<Demand> dem = mapDem.get(key);
                        if (dem == null) {
                            mapDem.put(key, dem = new HashSet<>());
                        }
                        dem.add(demandKey);

                        //Group puerchase orders by poID
                        Set<Demand> d = mapPO.get(poNum);
                        if (d == null) {
                            mapPO.put(poNum, d = new HashSet<>());
                        }
                        d.add(demandKey);
                        break;
                    }
                }
            } // end of while loop

            if (flowerDemand.isEmpty()) {
                System.out.println("\nDemand Set is empty at start");
            } else {
                System.out.print("\nDemand Set was created and contains "
                        + flowerDemand.size() + " elements.");
            }

            if (mapPO.isEmpty()) {
                System.out.println("\nmapPO is empty at start");
            } else {
                System.out.print("\nmapPO was created and contains "
                        + mapPO.size() + " elements.");
            }

            return true;
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName.toString());
            return false;
        } catch (IOException e) {
            System.out.println("Unable to read file: " + fileName.toString());
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println("Unable to read file: " + fileName.toString());
                    return false;
                }
            }
        } // end try catch reading file demand
    }

    String assortedDist(String[] purchase, String varFwr) {
        String prodForAssorted = "Product";
        switch (varFwr) {
            case "ASRO":
                prodForAssorted = "Rose";
                break;
            case "ASCA":
                prodForAssorted = "Carnation";
                break;
            case "ASMC":
                prodForAssorted = "Minicarnation";
                break;
        }
        String colorProd = purchase[7];
        for (Map.Entry<Integer, FlowerType> entry : mapP.entrySet()) {
            int key = entry.getKey();
            FlowerType value = entry.getValue();
            if (mapP2.get(key).equals(prodForAssorted)
                    && mapPCol.get(key).equals(colorProd)) {
                varFwr = value.getVariety();
                //System.out.println(prodForAssorted + ", " + colorProd + ", " + varFwr);
                break;
            }
        }
        return varFwr;
    }

    void offerReq() {
        for (Map.Entry<Integer, Set<Demand>> entryD : mapDem.entrySet()) {
            int key = entryD.getKey();
            for (Offer offerNeed : offerInflow) {
                int keyO = offerNeed.getKeyProduct();
                if (key == keyO) {
                    String varFwrO = offerNeed.getVariety();
                    int intLengthO = offerNeed.getLength();
                    int opennessFwrO = offerNeed.getOpenness();
                    int periodInflow = offerNeed.getTime();
                    int inflowFwr = offerNeed.getQty();
                    String unitFwr = offerNeed.getUnit();

                    Offer offerReq = new Offer(keyO, varFwrO, intLengthO,
                            opennessFwrO, periodInflow, inflowFwr, unitFwr);

                    Set<Offer> productReq = offerInflowReq.get(key);
                    if (productReq == null) {
                        offerInflowReq.put(key, productReq = new HashSet<>());
                    }
                    productReq.add(offerReq);
                }
            }
        }
    }

    void peopleReq() {
        Map<Integer, Integer> mapR = new LinkedHashMap<>();
        int peopleR = 0, peopleC = 0, peopleMC = 0, peopleB = 0;
        int stemCapty = (minutesPeriod * 60 / stemArrange);
        int boxCapty = (minutesPeriod * 60 / boxArrange);
        int roseAccum = 0, carnAccum = 0, mcarnAccum = 0, boxAccum = 0;
        for (Demand demandPO : flowerDemand) {
            int keyAtD = demandPO.getKeyProduct();
            int productQty = demandPO.getPoStemsQty();
            int boxQty = (int) demandPO.getPoBoxQty();
            switch (mapP2.get(keyAtD)) {
                case "Rose":
                    roseAccum = roseAccum + productQty;
                    mapR.put(1, roseAccum);
                    break;
                case "Carnation":
                    carnAccum = carnAccum + productQty;
                    mapR.put(2, carnAccum);
                    break;
                case "Minicarnation":
                    mcarnAccum = mcarnAccum + productQty;
                    mapR.put(3, mcarnAccum);
                    break;
            }
            boxAccum = boxAccum + boxQty;
            mapR.put(4, boxAccum);
        }
        for (Map.Entry<Integer, Integer> entry : mapR.entrySet()) {
            int productKey = entry.getKey();
            int productQty = entry.getValue();
            //System.out.println(productKey + ", " + productQty);
            switch (productKey) {
                case 1:
                    peopleR = (int) ((productQty / stemCapty) / periodsInt * 1.3);
                    break;
                case 2:
                    peopleC = (int) ((productQty / stemCapty) / periodsInt * 1.3);
                    break;
                case 3:
                    peopleMC = (int) ((productQty / stemCapty) / periodsInt * 1.3);
                    break;
                case 4:
                    peopleB = (int) ((productQty / boxCapty));
                    peopleB = peopleB + 1;
                    break;
            }
        }
        System.out.println("\n\nPeople Required:\n" + peopleR
                + " for Roses\n" + peopleC + " for Carnation\n"
                + peopleMC + " for Minicarnation\n" + peopleB + " for Boxes");
        peopleRose = peopleR;
        peopleCarnation = peopleC;
        peopleMini = peopleMC;
        peopleBox = peopleB;
    }

    void getPtyVal() {
        keyP = 0;
        tPty = new int[mapPO.size()][periodsUsed];
        for (Map.Entry<Integer, Set<Demand>> entryPO : mapPO.entrySet()) {
            int cPO = entryPO.getKey();
            Set<Demand> cValues = entryPO.getValue();
            int maxPtyPO = 0;
            int increment = 0;
            for (int t = 0; t < periodsUsed; t++) {
                Iterator itr = cValues.iterator();
                while (itr.hasNext()) {
                    Object element = itr.next();
                    for (Demand element2 : flowerDemand) {
                        if (element.equals(element2)) {
                            if (t < breakPoint1) {
                                maxPtyPO = 0;
                            } else if (t < breakPoint2) {
                                if (maxPtyPO < element2.getPty1()) {
                                    maxPtyPO = element2.getPty1();
                                }
                            } else {
                                if (maxPtyPO < element2.getPty2()) {
                                    maxPtyPO = element2.getPty2();
                                }
                            }
                        }
                    }
                }
                tPty[keyP][t] = maxPtyPO + increment;
                //System.out.println("Penalty " + keyP + ", " + t + " = " + tPty[keyP][t]);
                increment += 5;
            }
            mapPO2.put(keyP, cPO);
            keyP++;
        }//*/
    }

    boolean getFeasibility() {
        boolean feasibleCheck = true;
        int difference;

        for (Map.Entry<Integer, Set<Demand>> entry : mapDem.entrySet()) {
            int keyReq = entry.getKey();
            int stemsReq = 0;
            Iterator itrD = entry.getValue().iterator();
            while (itrD.hasNext()) {
                Object element = itrD.next();
                Demand prodVal = (Demand) element;
                stemsReq = stemsReq + prodVal.stemsBox;
            }
            int stemsOffer = 0;
            for (Offer offerIw : offerInflowStems) {
                if (keyReq == offerIw.getKeyProduct()) {
                    stemsOffer = stemsOffer + offerIw.getQty();
                }
            }
            difference = stemsOffer - stemsReq;
            if (difference < 0) {
                System.out.println("\n\nInfeasible: " + mapP.get(keyReq));
                System.out.println("Offer is " + stemsOffer
                        + " and Demand is " + stemsReq);
                feasibleCheck = false;
            }
        }
        System.out.print("\n\n\tFeasibility Check : ");
        if (feasibleCheck == true) {
            return feasibleCheck;
        } else {
            return feasibleCheck;
        }
    }  //end of getFeasibility method

    void earlierTimePeriodPO() {
        int countL = 0;

        for (Map.Entry<Integer, Set<Demand>> entryPO : mapPO.entrySet()) {
            int keyAtPO = entryPO.getKey();
            int timePeriod = 0;
            for (Demand dem : entryPO.getValue()) {
                int flowerValD = dem.getKeyProduct();
                double demandFw = dem.getPoStemsQty();
                double flowerInw = 0;
                int timePeriodF = 0;
                mapEarlyTP1.clear();
                //System.out.println("product " + flowerValD);
                for (Offer ofr : offerInflowStems) {
                    int flowerValO = ofr.getKeyProduct();
                    if (flowerValD == flowerValO) {
                        flowerInw = flowerInw + ofr.getQty();
                        mapEarlyTP1.put(ofr.getTime(), flowerInw);
                        //System.out.println("offer: " + flowerInw + " @ tp " 
                        //        + ofr.getTime() + " ; required " + demandFw);
                    }
                }
                for (Map.Entry<Integer, Double> entryTP : mapEarlyTP1.entrySet()) {
                    int timePrd = entryTP.getKey();
                    double cumQty = entryTP.getValue();
                    if (cumQty - demandFw >= 0) {
                        timePeriodF = timePrd;
                        //System.out.println(keyAtPO + ", found = " + timePeriodF);
                        break;
                    }
                }
                if (timePeriodF >= timePeriod) {
                    timePeriod = timePeriodF;
                    //System.out.println(keyAtPO + ", " + timePeriod);
                }
            }
            mapEarlyTP.put(keyAtPO, timePeriod);
        }
        for (Map.Entry<Integer, Integer> entryTP : mapEarlyTP.entrySet()) {
            int poId = entryTP.getKey();
            //int eTp = entryTP.getValue();
            //System.out.println(poId + ", " + eTp);
            mapPOint.put(poId, countL); //poID = 2459630 & countL = 3
            countL++;
        }

        if (mapEarlyTP.isEmpty()) {
            System.out.println("\nMap Earliest time is empty at start");
        } else {
            System.out.println("\nmap Earliest time was created.");
        }
    }

    void printMapsNsets() {
        input = new Scanner(System.in);
        System.out.print("\nDo you want to print Maps and Sets? Y/N\t");
        printMapNset = input.next();
        System.out.println("\nWorking periods per day : " + periodsInt);
        System.out.println("Last Pick-up in period : " + periodsUsed);
        System.out.println("First Break Point : " + breakPoint1);
        System.out.println("Second Break Point : " + breakPoint2);
        System.out.println("Personnel at Rose lane : " + peopleRose);
        System.out.println("Personnel at Carnation lane : " + peopleCarnation);
        System.out.println("Personnel at Minicarnation lane : " + peopleMini);
        System.out.println("Personnel at Boxing stage : " + peopleBox);

        System.out.println("\nMaximum CAPACITY of:\nRoses to bunch per period = " + roseBunchCapacity);
        System.out.println("Carnations to bunch per period = " + carnationBunchCapacity);
        System.out.println("Minicarnations to bunch per period = " + miniBunchCapacity);
        System.out.println("Bunches to box per period = " + boxCapacity);
        System.out.println("_________________________________________________________\n");

        System.out.println("\nProduct requested:");

        if (printMapNset.equalsIgnoreCase("Y")) {
            printMapNset = "N";
            System.out.print("\nConversion map desplay? Y/N\t");
            printMapNset = input.next();
            if (printMapNset.equalsIgnoreCase("Y")) {
                printMapNset = "N";
                System.out.println("\nConversion Map \nKey: longKey\tSTEMS/BUNCH\tBUNCH/BOX\tSTEMS/BOX");
                for (Map.Entry<String, Conversion> entry : mapCv2.entrySet()) {
                    String key = entry.getKey();
                    Conversion value = entry.getValue();
                    System.out.println(key + ": " + value);
                }
                for (Map.Entry<Integer, String> entry : mapBx1.entrySet()) {
                    Integer key = entry.getKey();
                    String value = entry.getValue();
                    System.out.println(key + ": " + value);
                }
            }

            System.out.print("\nProduct map desplay? Y/N\t");
            printMapNset = input.next();
            if (printMapNset.equalsIgnoreCase("Y")) {
                printMapNset = "N";
                System.out.println("\nProduct Map \nKey:\tPRODUCT/ COLOR/ VARIETY/ LENGTH/ OPENNESS");
                for (Map.Entry<Integer, FlowerType> entry : mapP.entrySet()) {
                    int key = entry.getKey();
                    FlowerType value = entry.getValue();
                    System.out.println(key + ": " + mapP2.get(key)
                            + ", " + mapPCol.get(key) + ", " + value);
                }
            }

            System.out.print("\nDemand Set desplay? Y/N\t");
            printMapNset = input.next();
            if (printMapNset.equalsIgnoreCase("Y")) {
                printMapNset = "N";
                System.out.println("\nDemand Set \nKey:\tVARIETY/ LENGTH/ OPENNESS/ "
                        + "BOXTYPE/ PO_TYPE/ PO/ QTY/ STEM_BUNCH/ BUNCH_BOX/ STEMS_BOX/ TOTAL_STEMS/ PTY1/ PTY2");
                for (Demand element : flowerDemand) {
                    System.out.println(element);
                }
            }

            System.out.print("\nPO map desplay? Y/N\t");
            printMapNset = input.next();
            if (printMapNset.equalsIgnoreCase("Y")) {
                printMapNset = "N";
                System.out.println("\nMap PO \nPO:\tDemand Set for each PO");
                for (Map.Entry<Integer, Set<Demand>> entryPO : mapPO.entrySet()) {
                    for (Map.Entry<Integer, Integer> entryPO2 : mapPO2.entrySet()) {
                        int keyAtPO = entryPO.getKey(); //poID as key at mapPO
                        int keyAtPO2 = entryPO2.getKey(); // client k (enum)
                        int valAtPO2 = entryPO2.getValue(); //poID as value at mapPO2
                        if (keyAtPO == valAtPO2) {
                            System.out.print(keyAtPO2 + ": " + keyAtPO + ": ");
                            for (Demand dem : entryPO.getValue()) {
                                System.out.print("{" + dem + "},");
                            }
                            System.out.println();
                        }
                    }
                }
            }

            System.out.print("\nRequired Offer Set desplay? Y/N\t");
            printMapNset = input.next();
            if (printMapNset.equalsIgnoreCase("Y")) {
                printMapNset = "N";
                System.out.println("\nOffer Set \nKey:\tVARIETY/ LENGTH/ OPENNESS/ TIME/ QTY/ UNIT");
                for (Map.Entry<Integer, Set<Offer>> element : offerInflowReq.entrySet()) {
                    int key = element.getKey();
                    Iterator itr = element.getValue().iterator();
                    System.out.print("\n" + key + ", ");
                    while (itr.hasNext()) {
                        Object entry = itr.next();
                        System.out.print("{" + entry + "} ");
                    }
                }
            }

            System.out.print("\n\nMap with earliest time periods desplay? Y/N\t");
            printMapNset = input.next();
            if (printMapNset.equalsIgnoreCase("Y")) {
                printMapNset = "N";
                System.out.println("\nMap with earliest time periods for each PO id");
                for (Map.Entry<Integer, Integer> entryTP : mapEarlyTP.entrySet()) {
                    int key = entryTP.getKey();
                    int value = entryTP.getValue();
                    System.out.println(key + ", " + value);
                }
                System.out.println("\nMap with earliest time periods for each PO consecutive int numbers");
                for (Map.Entry<Integer, Integer> entryTP : mapPOint.entrySet()) {
                    int key = entryTP.getKey();
                    int value = entryTP.getValue();
                    System.out.println(key + ", " + value);
                }
            }

            System.out.print("\nPenalty vector display? Y/N\t");
            printMapNset = input.next();
            if (printMapNset.equalsIgnoreCase("Y")) {
                printMapNset = "N";
                System.out.println("\nPenalty vector tPty[po][t]");
                Iterator itrPty = mapPO2.values().iterator();
                while (itrPty.hasNext()) {
                    Object entry = itrPty.next();
                    int poNum = (int) entry;
                    System.out.println("\n" + entry + ": ");
                    for (int t = 0; t < periodsUsed; t++) {
                        System.out.print(tPty[mapPOint.get(poNum)][t] + " ");
                    }
                }
                System.out.println();
            }
        }
    }

    void generateDemand(int demFVal) {
        //method call at the end of getFlowerParameters()
        System.out.println("\nVARIETY,LENGTH,OPENNESS,BoxSize,"
                + "OrderType,PO,BoxQty,color,StemBunch,BunchBox,StemBox,TotalStem,random");
        int poID = (int) (2459000 + Math.random() * 900);
        String orderType;
        for (int k = 0; k < demFVal; k++) {
            poID = poID + 1;
            for (int po = 0; po < (1 + Math.rint(Math.random() * 10)); po++) {
                int varChoose = (int) Math.rint(4575 * Math.random());
                FlowerType f_Var = mapP.get(varChoose);
                String var = f_Var.getVariety();
                int fOpen = f_Var.getOpenness();
                String product = mapP2.get(varChoose);
                String color = mapPCol.get(varChoose);
                int fLength = f_Var.getLength();
                if (fLength < 10) {
                    String fLengthS = "STANDARD";
                    switch (fLength) {
                        case 1:
                            fLengthS = "STANDARD";
                            if (product.equals("Minicarnation")) {
                                fLength = 2;
                                fLengthS = "FANCY";
                            }
                            break;
                        case 2:
                            fLengthS = "FANCY";
                            break;
                        case 3:
                            fLengthS = "SELECT";
                            break;
                    }
                    System.out.print(var + "," + fLengthS);
                } else {
                    System.out.print(var + "," + fLength);
                }
                double rNdom = Math.random();
                double rNdom2 = Math.random();
                int boxQty;
                String bSize;
                if (rNdom <= 0.6) {
                    bSize = "HB";
                } else if (rNdom > 0.6 && rNdom < 0.8) {
                    bSize = "QB";
                } else {
                    bSize = "FB";
                }
                double rN = Math.random();
                if (rN <= 0.35) {
                    orderType = "F";
                } else if (rN > 0.35 && rN <= 0.6) {
                    orderType = "R";
                } else {
                    orderType = "M";
                }
                boxQty = (int) (rNdom2 * 10) + 1;
                String convKey = product + bSize + fLength;
                Conversion fConv = mapCv2.get(convKey);
                int stemBunchC = fConv.getStemBunch();
                int bunchBoxC = fConv.getBunchBox();
                int stemBoxC = fConv.getStemBox();
                int stemTotal = boxQty * stemBoxC;
                System.out.println("," + fOpen
                        + "," + bSize + "," + orderType + "," + poID
                        + "," + boxQty + "," + color + "," + stemBunchC
                        + "," + bunchBoxC + "," + stemBoxC
                        + "," + stemTotal + "," + k);
            }
        }
    }

    void generateOffer(String varP, int fLength, int fOpen) {
        String flowerLgth = "FANCY";
        boolean lengthInt = false;
        switch (fLength) {
            case 3:
                flowerLgth = "SELECT";
                break;
            case 2:
                flowerLgth = "FANCY";
                break;
            case 1:
                flowerLgth = "STANDARD";
                break;
            default:
                lengthInt = true;
                break;
        }
        int qtyArrive = 0;
        int startArrive = (int) (Math.random() * 1 + Math.random() * 4);
        if (Math.random() < 0.13) {
            startArrive = 0;
        }
        for (int t = startArrive; t < 8; t++) {
            System.out.print(varP + ",");
            if (lengthInt) {
                System.out.print(fLength + "," + fOpen);
            } else {
                System.out.print(flowerLgth + "," + fOpen);
            }
            if (t == 0) {
                qtyArrive = (int) (5 * Math.random() + 10 * Math.random());
                System.out.print(",Bunch,");
            } else {
                if (Math.random() < 0.5) {
                    t = t + (int) (Math.random() * 3);
                }
                qtyArrive = (int) (840 * Math.random() + 1300 * Math.random()
                        + 2700 * Math.random());
                System.out.print(",Stem,");
            }
            int tpArrive = t * minutesPeriod;
            System.out.print(qtyArrive + "," + tpArrive + "\n");
        }
    }
}  //end class FlowerData  

