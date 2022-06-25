/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packingoperation;

import java.util.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author andresposadac
 */
public class Lns {

    //binary y_var is the vector of Purchase Orders, 1 = not served and 0 = otherwise
    //binary x_var is the vector of Purchase Orders batched at time period, 1 = batched and 0 = otherwise
    //after reading the demand, set poDemand has the # of PO and -1 to start counting from 0
    //periodsUse are the time intervals and -1 to start counting from 0
    double etpProb = 0.65, mipGAP;
    FlowerData flowerRef;
    int horizon, clients, objF, yCount, xCount, quarter, optMethod;
    int chooseTP = 0;
    int[] y_var, y_varInc;
    Set<Integer> k_set = new TreeSet<>();
    int[][] x_var, x_varInc;
    double p_f0, p_f1, p_f2;
    double p_q1, p_q2, p_q3, p_q4;
    int repCount = 0, desCount = 0;
    int repY, desX;
    Set<Integer> servedK = new TreeSet<>();
    Set<Integer> notServedK = new TreeSet<>();
    Set<Integer> repairedPO = new TreeSet<>();
    Set<Integer> destroyedPO = new TreeSet<>();
    Set<Integer> destroyableX = new TreeSet<>();
    int[] distriTime;

    public Lns(FlowerData flowerRef) {
        this.flowerRef = flowerRef;
        // System.out.println("\nPassing object from FlowerData into Lns: ");
    }

    int createVar() {
        horizon = flowerRef.periodsUsed;
        clients = flowerRef.mapPO.size();
        objF = (flowerRef.bigPty * clients);
        y_var = new int[clients];
        y_varInc = new int[clients];
        x_var = new int[clients][horizon];
        x_varInc = new int[clients][horizon];
        quarter = (int) (horizon / 4);
        return clients;
    }

    void trivialPattern() {
        System.out.println("\t** Refresh Trivial Pattern **");
        for (Map.Entry<Integer, Integer> entryPO : flowerRef.mapPO2.entrySet()) {
            int c = entryPO.getKey();
            //int cPO = entryPO.getValue();
            y_var[c] = 1;       //not serving any purchase order
            for (int t = 0; t < horizon; t++) {
                x_var[c][t] = 0;        //not batching any PO
                //System.out.println("x(" + c + "," + t + ") = " + x_var[c][t]);
            }
            //System.out.println("y(" + c + ") = " + y_var[c]);
        }
        objF = objF_call();
    }

    void assignMIPPattern() {
        useMIPsolution();
        for (int c = 0; c < clients; c++) {
            y_var[c] = 1;
            for (int t = 0; t < horizon; t++) {
                if (x_var[c][t] > 0) {
                    y_var[c] = 0;
                    break;
                }
            }
        }
        verifyPattern();
        incumbentPattern();
    }

    void useMIPsolution() {
        //Paste MIP results in here!!
    }

    int detProb() {
        //System.out.println("\nProbabilities");
        patternSets();
        verifyPattern();
        yCount = notServedK.size();
        xCount = servedK.size();
        int q1 = 0, q2 = 0, q3 = 0, q4 = 0;
        for (int t = 0; t < horizon; t++) {
            if (t <= quarter) {
                q1 = q1 + distriTime[t];
            } else if (t > quarter && t <= quarter * 2) {
                q2 = q2 + distriTime[t];
            } else if (t > quarter * 2 && t <= quarter * 3) {
                q3 = q3 + distriTime[t];
            } else {
                q4 = q4 + distriTime[t];
            }
        }

        int cnt_f0 = 0, cnt_f1 = 0, cnt_f2 = 0;
        for (Integer cNum : servedK) {
            for (int t = 0; t < horizon; t++) {
                if (x_var[cNum][t] == 1) {
                    if (t < flowerRef.breakPoint1) {
                        cnt_f0++;
                    } else if (t < flowerRef.breakPoint2) {
                        cnt_f1++;
                    } else {
                        cnt_f2++;
                    }
                }
            }
        }

        if (xCount > 0) {
            p_q1 = q1 / (double) xCount;
            p_q2 = q2 / (double) xCount;
            p_q3 = q3 / (double) xCount;
            p_q4 = q4 / (double) xCount;
            p_f0 = cnt_f0 / (double) xCount;
            p_f1 = cnt_f1 / (double) xCount;
            p_f2 = cnt_f2 / (double) xCount;
        } else {
            p_q1 = 1 / quarter;
            p_q2 = p_q1;
            p_q3 = p_q2;
            p_q4 = 1.0 - (p_q1 * 3);
            p_f0 = 0.35;
            p_f1 = 0.34;
            p_f2 = 0.31;
        }
        objF = objF_call();
        /*
         System.out.println(p_q1 + ", " + p_q2 + ", "
         + p_q3 + ", " + p_q4 + " :objF = " + objF);
         System.out.println(p_f0 + ", " + p_f1 + ", " 
         + p_f2 + " :objF = " + objF);//*/
        return objF;
    }

    void verifyPattern() {
        patternSets();
        for (Integer c : servedK) {
            if (notServedK.contains(c)) {
                System.out.println("\n\n\n******* WARNING Same client ("
                        + c + ") served and not served *****\n"
                        + "must fix\n\n");
                fixPattern(c);
            } else {
                int cntX = 0;
                for (int t = 0; t < horizon; t++) {
                    if (x_var[c][t] == 1) {
                        cntX++;
                    }
                }
                if (cntX != 1) {
                    if (cntX < 1) {
                        System.out.println("\n\n\n******* " + "WARNING Client ("
                                + c + ") not registered *****\n"
                                + "must fix\n\n");
                    } else {
                        System.out.println("\n\n\n******* " + "WARNING Client ("
                                + c + ") registered more than once *****\n"
                                + "must fix\n\n");
                    }
                    fixPattern(c);
                }
            }
        }
    }

    void fixPattern(int kFix) {
        y_var[kFix] = 1;
        for (int t = 0; t < horizon; t++) {
            x_var[kFix][t] = 0;
        }
        System.out.println("Fixed for client (" + kFix
                + ") now y(" + kFix + ") = 1");
    }

    void patternSets() {
        servedK.clear();
        notServedK.clear();
        for (int c = 0; c < clients; c++) {
            if (y_var[c] == 1) {
                notServedK.add(c);
            } else {
                servedK.add(c);
            }
        }
    }

    void clearTempSets() {
        repairedPO.clear();
        destroyedPO.clear();
    }

    int objF_call() {
        verifyPattern();
        distriTime = new int[flowerRef.periodsUsed];
        int sumPty = 0;
        for (int c = 0; c < clients; c++) {
            for (int t = 0; t < horizon; t++) {
                if (x_var[c][t] == 1) {
                    int numPty = flowerRef.tPty[c][t];
                    sumPty = sumPty + numPty;
                    distriTime[t] = distriTime[t] + 1;
                }
            }
            if (y_var[c] == 1) {
                sumPty = sumPty + flowerRef.bigPty;
            }
        }
        //System.out.println("objF_call = " + sumPty);
        return sumPty;
    }

    int select_K(String operatorType) {
        patternSets();
        verifyPattern();
        patternSets();
        int selOneK;
        int cnt = 0;
        int kFound = 0;
        switch (operatorType) {
            case "repair":
                if (notServedK.isEmpty()) {
                    break;
                }
                selOneK = (int) Math.round((notServedK.size() * Math.random()));
                if (selOneK == 0) {
                    selOneK = 1;
                }
                for (Integer c : notServedK) {
                    cnt++;
                    if (cnt == selOneK) {
                        kFound = c;
                        break;
                    }
                }
                break;
            case "destroy":
                if (servedK.isEmpty()) {
                    break;
                }
                selOneK = (int) Math.round((servedK.size() * Math.random()));
                if (selOneK == 0) {
                    selOneK = 1;
                }
                for (Integer c : servedK) {
                    cnt++;
                    if (cnt == selOneK) {
                        kFound = c;
                        break;
                    }
                }
                break;
        }
        return kFound;
    }

    int repairOperator(int roType) {
        //System.out.println("\nRepair Operator");
        int kFound = select_K("repair");
        if (notServedK.isEmpty()) {
            System.out.println("\nNothing to repair from");
        } else {
            int eTp = flowerRef.mapEarlyTP.get(flowerRef.mapPO2.get(kFound));
            int tp = eTp + repType(eTp, roType);
            y_var[kFound] = 0;
            repY = kFound;
            chooseTP = tp;
            for (int t = 0; t < flowerRef.periodsUsed; t++) {
                if (t == tp) {
                    x_var[kFound][t] = 1;
                    //System.out.println("x(" + kFound + "," + t + ")");
                } else {
                    x_var[kFound][t] = 0;
                }
            }
        }
        repairedPO.add(repY);
        return objF_call();
    }

    int repType(int eTp, int typeRO) {
        int range_f = 0;
        double pickEtp = 0.55;
        double rN = Math.random();
        String methodExp = "methodExp";
        switch (typeRO) {
            case 0:
                methodExp = "Order";
                range_f = (int) Math.round((horizon - eTp) * Math.random());
                for (int t = eTp; t < flowerRef.periodsUsed; t++) {
                    int cntServed = 0;
                    for (Integer c : servedK) {
                        if (x_var[c][t] == 1) {
                            cntServed++;
                        }
                    }
                    if (cntServed <= 1) {
                        range_f = cntServed;
                        break;
                    }
                }
                break;
            case 1:
                methodExp = "Random";
                range_f = (int) Math.round((horizon - eTp) * Math.random());
                break;
            case 2:
                methodExp = "Penalty Oriented Reversed";
                if (rN <= p_f0) {
                    range_f = (int) Math.round((horizon - eTp) * Math.random());
                } else if (rN >= p_f0 && rN <= (p_f0 + p_f1)) {
                    range_f = (int) Math.round((flowerRef.breakPoint2 - eTp) * Math.random());
                } else if (rN > 1 - p_f2) {
                    range_f = (int) Math.round((flowerRef.breakPoint1 - eTp) * Math.random());
                }
                break;
            case 3:
                methodExp = "Less Penalty";
                if (rN <= p_f0) {
                    //System.out.println("rN < p_f0 with eTp= " + eTp);
                    if (flowerRef.breakPoint1 < eTp) {
                        range_f = (int) Math.round((flowerRef.breakPoint2 - eTp) * Math.random());
                    } else {
                        if (Math.random() <= pickEtp) {
                            range_f = 0;
                        } else {
                            range_f = (int) Math.round((flowerRef.breakPoint1 - eTp) * Math.random());
                        }
                    }
                } else if (rN >= p_f0 && rN <= (p_f0 + p_f1)) {
                    //System.out.println("rN >= p_f0 && rN <= (p_f0 + p_f1) with eTp= " + eTp);
                    range_f = (int) Math.round((flowerRef.breakPoint2 - eTp) * Math.random());
                } else if (rN > 1 - p_f2) {
                    //System.out.println("rN > 1 - p_f2 with eTp= " + eTp);
                    range_f = (int) Math.round((horizon - eTp) * Math.random());
                }
                break;
            case 4:
                methodExp = "Workload Oriented Reversed";
                if (rN < p_q1) {
                    range_f = (int) Math.round((horizon - eTp) * Math.random());
                } else if (rN >= p_q1 && rN <= (p_q1 + p_q2)) {
                    range_f = (int) Math.round(((quarter * 3) - eTp) * Math.random());
                } else if (rN >= (p_q1 + p_q2) && rN <= (p_q1 + p_q2 + p_q3)) {
                    range_f = (int) Math.round(((quarter * 2) - eTp) * Math.random());
                } else {
                    range_f = (int) Math.round((quarter - eTp) * Math.random());
                }
                break;
            case 5:
                methodExp = "Less Workload";
                if (rN < p_q1) {
                    if (Math.random() <= pickEtp) {
                        range_f = 0;
                    } else {
                        range_f = (int) Math.round((quarter - eTp) * Math.random());
                    }
                } else if (rN >= p_q1 && rN <= (p_q1 + p_q2)) {
                    range_f = (int) Math.round(((quarter * 2) - eTp) * Math.random());
                } else if (rN >= (p_q1 + p_q2) && rN <= (p_q1 + p_q2 + p_q3)) {
                    range_f = (int) Math.round(((quarter * 3) - eTp) * Math.random());
                } else {
                    range_f = (int) Math.round((horizon - eTp) * Math.random());
                }
                break;
        }
        if (range_f < 0 || (range_f + eTp) > horizon) {
            range_f = 0;
        }
        //System.out.println(methodExp + " rangeTP =(" + range_f + ")\t");
        return Math.round(range_f);
    }

    int destroyOperatorRev(int desOperType) {
        patternSets();
        verifyPattern();
        patternSets();
        if (servedK.isEmpty()) {
            System.out.println("\nNothing to destroy from");
        } else {
            desX = destroyType(desOperType);
        }
        destroyedPO.add(desX);
        return objF_call();
    }

    int destroyType(int typeDOp) {
        int upperTp = horizon;
        int lowerTp = 0;
        destroyableX.clear();
        double rN = Math.random();
        int xFound = 0;
        String methodExp = "methodExp";
        switch (typeDOp) {
            case 1:
                methodExp = "Random";
                xFound = select_K("destroy");
                destroyableX.add(xFound);
                break;
            case 2:
                methodExp = "Penalty Oriented";
                xFound = select_K("destroy");
                if (rN <= p_f0) {
                    upperTp = horizon;
                    lowerTp = flowerRef.breakPoint2;
                } else if (rN >= p_f0 && rN <= (p_f0 + p_f1)) {
                    upperTp = flowerRef.breakPoint2;
                    lowerTp = flowerRef.breakPoint1;
                } else if (rN > 1 - p_f2) {
                    upperTp = flowerRef.breakPoint1;
                    lowerTp = 0;
                }
                for (int t = lowerTp; t < upperTp; t++) {
                    for (Integer c : servedK) {
                        if (x_var[c][t] == 1) {
                            destroyableX.add(c);
                        }
                    }
                }
                break;
            case 3:
                methodExp = "Workload Oriented";
                xFound = select_K("destroy");
                if (rN < p_q1) {
                    upperTp = horizon;
                    lowerTp = quarter * 3;
                } else if (rN >= p_q1 && rN <= (p_q1 + p_q2)) {
                    upperTp = quarter * 3;
                    lowerTp = quarter * 2;
                } else if (rN >= (p_q1 + p_q2) && rN <= (p_q1 + p_q2 + p_q3)) {
                    upperTp = quarter * 2;
                    lowerTp = quarter;
                } else {
                    upperTp = quarter;
                    lowerTp = 0;
                }
                for (int t = lowerTp; t < upperTp; t++) {
                    for (Integer c : servedK) {
                        if (x_var[c][t] == 1) {
                            destroyableX.add(c);
                        }
                    }
                }
                break;
        }
        if (destroyableX.isEmpty()) {
            xFound = select_K("destroy");
        } else {
            int findX = (int) Math.round(destroyableX.size() * Math.random());
            if (findX == 0) {
                findX = 1;
            }
            int cntD = 0;
            for (Integer x : destroyableX) {
                cntD++;
                if (cntD == findX) {
                    xFound = x;
                    break;
                }
            }
        }
        for (int t = 0; t < horizon; t++) {
            x_var[xFound][t] = 0;
        }
        y_var[xFound] = 1;
        objF_call();
        //System.out.println(methodExp + " (" + xFound + ")\t");
        return xFound;
    }

    double lnsGap(String operatorType) {
        double gapLNS;
        gapLNS = 0.0;
        verifyPattern();
        switch (operatorType) {
            case "repair":
                gapLNS = ((double) notServedK.size() / (double) clients);
                break;
            case "destroy":
                gapLNS = ((double) servedK.size() / (double) clients);
                break;
        }
        //int served = servedK.size();
        //System.out.println("\nServed " + served + " clients : % = " + gapLNS);
        return gapLNS;
    }

    int incumbentPattern() {
        int sumPty = 0;
        for (int c = 0; c < clients; c++) {
            for (int t = 0; t < horizon; t++) {
                if (x_var[c][t] == 1) {
                    sumPty = sumPty + flowerRef.tPty[c][t];
                }
            }
            if (y_var[c] == 1) {
                sumPty = sumPty + flowerRef.bigPty;
            }
        }
        if (sumPty <= objF) {
            //System.out.println("\nNew Incumbent: " + objF + " >= " + sumPty + "\t: " + desCount);            
            objF = sumPty;
            for (int c = 0; c < clients; c++) {
                if (y_var[c] == 1) {
                    y_varInc[c] = 1;
                    for (int t = 0; t < horizon; t++) {
                        x_varInc[c][t] = 0;
                    }
                } else {
                    y_varInc[c] = 0;
                    for (int t = 0; t < horizon; t++) {
                        if (x_var[c][t] == 1) {
                            x_varInc[c][t] = 1;
                        } else {
                            x_varInc[c][t] = 0;
                        }
                    }
                }
            }
        }
        patternSets();
        return objF;
    }

    void refreshIncumbent() {
        System.out.print("\t** Refresh Incumbent **");
        int nsK = 0, sK = 0;
        for (int c = 0; c < flowerRef.mapPO.size(); c++) {
            if (y_varInc[c] == 1) {
                nsK++;
                y_var[c] = y_varInc[c];
                for (int t = 0; t < flowerRef.periodsUsed; t++) {
                    x_var[c][t] = 0;
                }
            } else {
                y_var[c] = 0;
                for (int t = 0; t < flowerRef.periodsUsed; t++) {
                    if (x_varInc[c][t] == 1) {
                        x_var[c][t] = x_varInc[c][t];
                        sK++;
                    } else {
                        x_var[c][t] = 0;
                    }
                }
            }
        }
        System.out.println("\t\tServed (" + sK + "/" + (sK + nsK)+")");
    }

    void printIncumbent() {
        System.out.println("\t** Printing Incumbent **");
        int sumPty = 0;
        for (int c = 0; c < clients; c++) {
            int kPO = flowerRef.mapPO2.get(c);
            if (y_varInc[c] == 1) {
                sumPty = sumPty + flowerRef.bigPty;
                //System.out.print("\n" + kPO + " :\t");
                //System.out.print(flowerRef.bigPty + "\ty(" + c + " \b)");
            } else {
                System.out.print("\n" + kPO + " :\t");

                for (int t = 0; t < horizon; t++) {
                    if (x_varInc[c][t] == 1) {
                        sumPty = sumPty + flowerRef.tPty[c][t];
                        System.out.print(flowerRef.tPty[c][t]
                                + "\t" + sumPty + "\tx(" + c + "," + t + ")");
                    }
                }
            }
            //System.out.println("\t\t" + sumPty);
        }
        System.out.println("\nTotal Penalty:\t" + sumPty + "\n");
    }

    void printDistribution() {
        patternSets();
        System.out.println("\nDistribution: ");
        int maxDist = 0;
        int clientsServed = 0;
        int cnt = 0;
        for (int t = 0; t < horizon; t++) {
            cnt++;
            if (cnt > horizon / 2) {
                cnt = 0;
                System.out.println();
            }
            if (distriTime[t] == 0) {
                System.out.print(" . ");
            } else {
                System.out.print(" " + distriTime[t] + " ");
                clientsServed = clientsServed + distriTime[t];
            }
            maxDist = Math.max(maxDist, distriTime[t]);
        }
        //System.out.println("\nServing = " + clientsServed + "\tMaxD = " + maxDist);
    }

    void printPattern() {
        System.out.println("\nPO\t\tPty\tCumPty\tSequence");
        int sumPty = 0;
        for (int c = 0; c < clients; c++) {
            int kPO = flowerRef.mapPO2.get(c);
            if (y_var[c] == 1) {
                sumPty = sumPty + flowerRef.bigPty;
                //System.out.print("\n" + kPO + " :\t");
                //System.out.print(flowerRef.bigPty + "\ty(" + c + " \b)");
            } else {
                System.out.print("\n" + kPO + " :\t");

                for (int t = 0; t < horizon; t++) {
                    if (x_var[c][t] == 1) {
                        sumPty = sumPty + flowerRef.tPty[c][t];
                        System.out.print(flowerRef.tPty[c][t]
                                + "\t" + sumPty + "\tx(" + c + "," + t + ")");
                    }
                }
            }
            //System.out.println("\t\t" + sumPty);
        }
        System.out.println("\nTotal Penalty:\t" + sumPty + "\n");
    }

    void printPatternFinal(FileWriter fw, int perform, Map<Integer, Integer> mapOF,
            Map<Integer, Long> mapOF2, Map<Integer, Integer> mapServedK,
            long startTimeLNS, long startTime,
            long endTime) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(fw)) {

            bw.write("Instance of " + clients + " clients\n"
                    + "Horizon = " + horizon + " time periods"
                    + "\n\nMinutes per period = " + flowerRef.minutesPeriod
                    + "\nAverage seconds to arrange stems = " + flowerRef.stemArrange
                    + "\nAverage seconds to arrange boxes = " + flowerRef.boxArrange
                    + "\n\nPeople Required:\n" + flowerRef.peopleRose
                    + " for Roses\n" + flowerRef.peopleCarnation + " for Carnation\n"
                    + flowerRef.peopleMini + " for Minicarnation\n"
                    + flowerRef.peopleBox + " for Boxes\n");

            if (optMethod == 1) {
                bw.write("\nMIP solved with a " + mipGAP + " GAP.\n");
            } else {
                bw.write("\nLNS\n");
            }

            int sumPty = 0;
            patternSets();
            xCount = servedK.size();
            yCount = notServedK.size();

            bw.write("\nPO\t\tPty\tCumPty\tSequence\n");

            for (int c = 0; c < clients; c++) {
                int kPO = flowerRef.mapPO2.get(c);
                if (y_var[c] == 1) {
                    sumPty = sumPty + flowerRef.bigPty;
                } else {
                    bw.write("\n" + kPO + " :\t");
                    for (int t = 0; t < horizon; t++) {
                        if (x_var[c][t] == 1) {
                            sumPty = sumPty + flowerRef.tPty[c][t];
                            bw.write(flowerRef.tPty[c][t] + "\t"
                                    + sumPty + "\tx(" + c + "," + t + ")");
                        }
                    }
                }
            }

            sumPty = 0;
            bw.write("\n\n\nPO\t\tPty\tComPty\tSequence\n");
            for (int t = 0; t < horizon; t++) {
                for (int c = 0; c < clients; c++) {
                    //System.out.println("x(" + c + "," + t + ") = " + x_var[c][t]);
                    if (x_var[c][t] == 1) {
                        int kPO = flowerRef.mapPO2.get(c);
                        sumPty = sumPty + flowerRef.tPty[c][t];
                        bw.write(kPO + " :\t" + flowerRef.tPty[c][t] + "\t" + sumPty
                                + "\tx(" + c + "," + t + ")\n");
                    }
                }
            }
            if (yCount != 0) {
                bw.write("\nClients not served:\n");
                for (int c = 0; c < clients; c++) {
                    if (y_var[c] == 1) {
                        int kPO = flowerRef.mapPO2.get(c);
                        sumPty = sumPty + flowerRef.bigPty;
                        bw.write(kPO + " :\t" + flowerRef.bigPty + "\t" + sumPty
                                + "\ty(" + c + ")\n");
                    }
                }
            }
            bw.write("\nTotal Penalty = " + sumPty + "\n");

            if (perform == 0) {
                bw.write("\nLNS:\n # \tPenalty\n");
                for (Map.Entry<Integer, Integer> resultsLNS : mapOF.entrySet()) {
                    int iter = resultsLNS.getKey();
                    int objFval = resultsLNS.getValue();
                    long secReg = mapOF2.get(iter);
                    int servedClients = mapServedK.get(iter);
                    int hourReg_0 = (int) (secReg / 3600);
                    int minReg_0 = (int) (secReg / 60) - (hourReg_0 * 60);
                    int secReg_0 = (int) secReg - (minReg_0 * 60);

                    bw.write(iter + "\t" + objFval + "\tat (" + hourReg_0
                            + ":" + minReg_0 + ":" + secReg_0 
                            + ")\t("+ servedClients + "/" + clients + ")\n");
                }
            }
            long totalTime = (endTime - startTime) / 1000;
            long totalTimeAcc = (endTime - startTimeLNS) / 1000;
            int totalTime_hours = (int) (totalTimeAcc / 3600);
            int totalTime_minLeft = (int) (totalTimeAcc / 60 - totalTime_hours * 60);
            int totalTime_secLeft = (int) (totalTimeAcc - totalTime_minLeft * 60
                    - totalTime_hours * 3600);
            int whatToPrint;
            if (totalTime_hours > 0) {
                whatToPrint = 3;
            } else if (totalTime_minLeft > 0) {
                whatToPrint = 2;
            } else {
                whatToPrint = 1;
            }
            bw.write("Solved in " + totalTime + " seconds\n");

            switch (whatToPrint) {
                case 1:
                    bw.write("Elapsed Time " + totalTime_secLeft + " seconds.");
                    break;
                case 2:
                    bw.write("Elapsed Time " + totalTime_minLeft
                            + " minutes and " + totalTime_secLeft + " seconds.");
                    break;
                case 3:
                    bw.write("Elapsed Time " + totalTime_hours + " hours, "
                            + totalTime_minLeft + " minutes, "
                            + totalTime_secLeft + " seconds.");
                    break;
            }
            bw.close();
        }
    }

    void printPatternByt() {
        System.out.println("\nPO\t\tPty\tCumPty\tSequence");
        int sumPty = 0;
        xCount = servedK.size();
        yCount = clients - xCount;

        for (int t = 0; t < horizon; t++) {
            for (int c = 0; c < clients; c++) {
                if (x_var[c][t] == 1) {
                    int kPO = flowerRef.mapPO2.get(c);
                    sumPty = sumPty + flowerRef.tPty[c][t];
                    System.out.println(kPO + " :\t" + flowerRef.tPty[c][t]
                            + "\t" + sumPty + "\tx(" + c + "," + t + ")");
                }
            }
        }
        if (yCount != 0) {
            System.out.println("\nClients not served:");
            for (int c = 0; c < clients; c++) {
                if (y_var[c] == 1) {
                    int kPO = flowerRef.mapPO2.get(c);
                    sumPty = sumPty + flowerRef.bigPty;
                    System.out.println(kPO + " :\t" + flowerRef.bigPty
                            + "\t" + sumPty + "\ty(" + c + ")");
                }
            }
        }
        System.out.println("\nTotal Penalty = " + sumPty + "\n");
    }

    void patternByBreakPoint() {
        //Prints number of clients served and not served at the moment
        //Identifying the penalty associated with each case and the total one
        int sumPty = 0;
        int sC = 0;
        int nsC = 0;
        patternSets();
        xCount = servedK.size();
        yCount = notServedK.size();
        System.out.println("\n\n*********");
        for (int t = 0; t < horizon; t++) {
            for (int c = 0; c < clients; c++) {
                if (x_var[c][t] == 1) {
                    sumPty = sumPty + flowerRef.tPty[c][t];
                    //System.out.print(c + ", ");
                    sC++;
                }
            }
        }
        System.out.print("\n** Serve " + sC + " clients");
        System.out.print("\tPenalty = " + sumPty + "\n");
        System.out.println("______________________________");
        if (yCount != 0) {
            for (int c = 0; c < flowerRef.mapPO.size(); c++) {
                if (y_var[c] == 1) {
                    sumPty = sumPty + flowerRef.bigPty;
                    //System.out.print(c + ", ");
                    nsC++;
                }
            }
            System.out.print("\n** Not serve " + nsC + " clients:");
            System.out.print("\tPenalty = " + (nsC * flowerRef.bigPty) + "\n");
            System.out.println("______________________________");
        }
        System.out.println("\nTotal Penalty = " + sumPty + "\n");
    }

    void printTrace(String operatorType) {
        int objV = objF_call();
        int kPO;
        int eTpFound;
        switch (operatorType) {
            case "repair":
                kPO = flowerRef.mapPO2.get(repY);
                eTpFound = flowerRef.mapEarlyTP.get(kPO);
                System.out.println(kPO + ": etp(" + eTpFound
                        + ") x(" + repY + "," + chooseTP + ")\tnow Obj = " + objV);
                break;
            case "destroy":
                kPO = flowerRef.mapPO2.get(desX);
                System.out.println(kPO + ": y(" + desX + ")\tnow Obj = " + objV);
                break;
        }
    }

    void displayPattern() {
        System.out.println();
        int ptyM = 0;
        int acumP = 0;
        int[] distributionTime = new int[flowerRef.periodsUsed];
        for (int c = 0; c < flowerRef.mapPO.size(); c++) {
            if (y_var[c] == 1) {
                //System.out.print(" (1) = 1000\n");
            } else {
                System.out.print(c + ":\t");
                for (int t = 0; t < flowerRef.periodsUsed; t++) {
                    if (x_var[c][t] == 1) {
                        System.out.print(" 1 ");
                        distributionTime[t] = distributionTime[t] + 1;
                        ptyM = flowerRef.tPty[c][t];
                        acumP = acumP + ptyM;
                    } else {
                        System.out.print(" . ");
                    }
                }
                System.out.print(" (0) = " + ptyM + "\n");
            }
        }
        System.out.print("\nDist:\t");
        int maxDist = 0;
        for (int t = 0; t < flowerRef.periodsUsed; t++) {
            System.out.print(" " + distributionTime[t] + " ");
            maxDist = Math.max(maxDist, distributionTime[t]);
        }

        System.out.print(" MaxD = " + maxDist + "\n");
    }

    void assignValues(String var, int c, int t) {
        //System.out.println("values to asign: " + var + "(" + c + "," + t + ")");
        switch (var) {
            case "y":
                y_var[c] = 1;
                //System.out.println("y(" + c + ") = 1 asign");
                break;
            case "x":
                y_var[c] = 0;
                //System.out.println("y(" + c + ") = 0 asign");
                for (int tp = 0; tp < horizon; tp++) {
                    if (tp == t) {
                        x_var[c][tp] = 1;
                        //System.out.println("x(" + c + "," + tp + ") = 1 asign");
                    }
                }
                break;
        }
        patternSets();
        objF_call();
    }

    void performMethod(int perform) {
        optMethod = perform;
    }

    void mipGap(double gAP) {
        mipGAP = gAP;
    }

    int highestPty() {
        return flowerRef.bigPty * clients;
    }
}
