/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packingoperation;

import ilog.concert.*;
import ilog.cplex.*;
import java.util.*;

/**
 *
 * @author andresposadac
 */
public class JobFlowModel {

    Lns fPattern;
    FlowerData fInfo;
    int status;
    int[][] valX;
    int[] valY;
    Scanner input = new Scanner(System.in);

    public JobFlowModel(FlowerData fInfo, Lns fPattern) {
        this.fInfo = fInfo;
        this.fPattern = fPattern;
    }

    public int packingFlow(int mIP, double gAP) throws IloException {
        //System.out.println("\nStart packingFlow check");
        int varSize = fInfo.mapP.size();
        int timeSize = fInfo.periodsUsed;
        int boxSize = fInfo.mapBx1.size(); // boxSize == 3; Q = 0, H = 1, F = 2 
        int poSize = fInfo.mapPO.size();
        int q1_rs = fInfo.roseBunchCapacity;
        int q1_rc = fInfo.carnationBunchCapacity;
        int q1_rmc = fInfo.miniBunchCapacity;
        int q2_s = fInfo.boxCapacity;
        int inflowStem;
        int inflowBunch;
        int inflowBox;
        String unit;
        long startingTimeModel;
        long startingTime;
        long endTime;
        boolean printTimeSpend = false;
        valY = new int[poSize];
        valX = new int[poSize][timeSize];

        if (mIP != 1) {
            mIP = 0;
        }
        //System.out.println("\nMIP = " + mIP);

        try {
            startingTimeModel = System.currentTimeMillis();
            startingTime = startingTimeModel;
            IloCplex cplex = new IloCplex();
            //System.out.println("\nmodel was created");

            IloNumVar[][] h1 = new IloNumVar[varSize][]; //stem inventory
            IloNumVar[][] h2 = new IloNumVar[varSize][]; //bunch inventory
            IloNumVar[][] u = new IloNumVar[varSize][]; //stems to bunches
            IloNumVar[][][] w = new IloNumVar[varSize][boxSize][]; // bunches to boxes
            IloNumVar[][][] h3 = new IloNumVar[varSize][boxSize][]; //box inventory
            IloNumVar[][][][] a = new IloNumVar[varSize][boxSize][poSize][]; // boxes to batches
            IloNumVar[][] x = new IloNumVar[poSize][]; //serve client in time period
            IloNumVar[] y = new IloNumVar[poSize]; //not serve client

            //create only variables for required products.
            for (Map.Entry<Integer, Set<Demand>> varReq : fInfo.mapDem.entrySet()) { //forall variety in demand map
                int v = varReq.getKey(); //variety key
                h1[v] = cplex.numVarArray(timeSize, 0.0, Double.MAX_VALUE);
                h2[v] = cplex.numVarArray(timeSize, 0.0, Double.MAX_VALUE);
                u[v] = cplex.numVarArray(timeSize, 0.0, Double.MAX_VALUE);
                for (int b = 0; b < boxSize; b++) { //forall boxes 
                    w[v][b] = cplex.numVarArray(timeSize, 0.0, Double.MAX_VALUE);
                    h3[v][b] = cplex.numVarArray(timeSize, 0.0, Double.MAX_VALUE);
                    Iterator itr = varReq.getValue().iterator();
                    while (itr.hasNext()) {  // per variety search at demand each request
                        Object entry = itr.next();
                        Demand vDemand = (Demand) entry;
                        int poIDk = vDemand.getPoId();
                        int k = fInfo.mapPOint.get(poIDk);
                        a[v][b][k] = cplex.numVarArray(timeSize, 0.0, Double.MAX_VALUE);
                    }
                }
            }

            if (mIP == 1) {  // MIP == 1 means Concert solve MIP
                for (int k = 0; k < poSize; k++) {   //forall clients            
                    x[k] = cplex.boolVarArray(timeSize); //x=1 serve, x=0 not serve at time timeSize
                    y[k] = cplex.boolVar(); // y=1 not serve, y=0 client served
                }

                cplex.setParam(IloCplex.DoubleParam.EpGap, gAP);
            } else { // MIP == 0 means Concert evaluates LP after LNS propose a pattern
                for (int k = 0; k < poSize; k++) {   //forall clients            
                    x[k] = cplex.numVarArray(timeSize, 0.0, 1.0); //x=1 serve, x=0 not serve at time timeSize
                    y[k] = cplex.numVar(0.0, 1.0); // y=1 not serve, y=0 client served
                }
                cplex.setParam(IloCplex.DoubleParam.EpGap, gAP);
                //cplex.setParam(IloCplex.BooleanParam.PreInd, false);
                cplex.setParam(IloCplex.IntParam.PPriInd, IloCplex.PrimalPricing.Steep);
                //cplex.setParam(IloCplex.IntParam.AggInd, 0);
                cplex.setParam(IloCplex.IntParam.PrePass, 2);
                cplex.setParam(IloCplex.IntParam.PreDual, -1);
            }

            endTime = System.currentTimeMillis();
            long timeSpend1 = (endTime - startingTime) / 1000;

            //OBJECTIVE: Min z = sum(over k,t) x[k][t]*P[k][t] + sum(over k)y[k]*bigM[k]
            IloLinearNumExpr serving = cplex.linearNumExpr();
            IloLinearNumExpr notServing = cplex.linearNumExpr();
            for (int k = 0; k < poSize; k++) {
                for (int t = 0; t < timeSize; t++) {
                    serving.addTerm(x[k][t], fInfo.tPty[k][t]);
                }
                notServing.addTerm(y[k], fInfo.bigPty);
            }
            cplex.addMinimize(cplex.sum(serving, notServing));

            /*CONSTRAINTS*/
            if (mIP == 0) {   // if true then execute LNS
                //System.out.println("\nPassing Pattern to decision variables x & y");
                for (int k = 0; k < poSize; k++) {
                    if (fPattern.y_var[k] == 1) {
                        cplex.addEq(y[k], 1, "LNS pattern y[" + k + "] = 1");
                        //System.out.println("y(" + k + ") = " + fPattern.y_var[k]);
                        for (int t = 0; t < timeSize; t++) {
                            cplex.addEq(x[k][t], 0, "LNS pattern x[" + k + "],[" + t + "] = 0");
                        }
                    } else {
                        cplex.addEq(y[k], 0, "LNS pattern y[" + k + "] = 0");
                        for (int t = 0; t < timeSize; t++) {
                            if (fPattern.x_var[k][t] == 1) {
                                cplex.addEq(x[k][t], 1, "LNS pattern x[" + k + "],[" + t + "] = 1");
                                //System.out.println("x(" + k + "," + t + ") = " + fPattern.x_var[k][t]);
                            } else {
                                cplex.addEq(x[k][t], 0, "LNS pattern x[" + k + "],[" + t + "] = 0");
                            }
                        }
                    }
                }
                //System.out.println("\npattern was passed to decision variables");
            } else {
                //System.out.println("\nbatching constraints were created");
                //SERVING OR NOT A CLIENTS REQUEST  sum(over t)x[k][t]=1-y[k]
                IloLinearNumExpr workTP = cplex.linearNumExpr();
                for (int k = 0; k < poSize; k++) { //forall k
                    //sum(over t)
                    for (int t = 0; t < timeSize; t++) {
                        workTP.addTerm(x[k][t], 1.0);
                    }
                    cplex.addEq(workTP, cplex.diff(1.0, y[k]), "Serve or Not PO: " + k);
                    workTP.clear();
                }
            }
            //System.out.println("\nDecision variables constraints were created");

            /*INVENTORY CONSTRAINTS*/
            startingTime = System.currentTimeMillis();
            IloLinearNumExpr bunchPerBox = cplex.linearNumExpr();  //sum(over b)w[v][b][t] 
            IloLinearNumExpr boxToBatch = cplex.linearNumExpr();  //sum(over k)a[v][b][k][t]

            /*offerInflowReq contains all varieties requested in Demand.
             * loop over this map using variety key extract inflow at particular time
             * and extract box size as well as poID required form Demand using mapDem */
            for (Map.Entry<Integer, Set<Offer>> offerR : fInfo.offerInflowReq.entrySet()) {
                int v = offerR.getKey();
                String longKeyP = fInfo.mapP2.get(v) + fInfo.mapP.get(v).getLength(); //conversion key for product and length
                Conversion convData = fInfo.mapCv1.get(longKeyP);
                int stems2bunch = convData.getStemBunch();  //s[v]
                for (int t = 0; t < timeSize; t++) {
                    inflowStem = 0;
                    inflowBunch = 0;
                    inflowBox = 0;
                    Iterator oItr = offerR.getValue().iterator();
                    while (oItr.hasNext()) {  // per variety search at demand each request
                        Object entry = oItr.next();
                        Offer vOffer = (Offer) entry;
                        unit = vOffer.getUnit();
                        int inflowTime = vOffer.getTime();
                        if (inflowTime == t) {
                            switch (unit) {
                                case "Stem":
                                    inflowStem = vOffer.getQty();
                                    break;
                                case "Bunch":
                                    inflowBunch = vOffer.getQty();
                                    break;
                                case "Box":
                                    inflowBox = vOffer.getQty();
                                    break;
                            }
                        } // if not found inflow will be zero for time period
                    }

                    if (t == 0) {
                        //F[v][0]-u[v][0]=h1[v][0]
                        cplex.addEq(inflowStem,
                                cplex.sum(h1[v][t], u[v][t]),
                                "H1 (" + v + ")(" + t + ")");
                    } else {
                        //h1[v][t-1]+F[v][t]-u[v][t]=h1[v][t] 
                        cplex.addEq(cplex.sum(h1[v][t - 1], inflowStem),
                                cplex.sum(h1[v][t], u[v][t]),
                                "h1 (" + v + ")(" + t + ")");
                    }

                    Iterator dItr = fInfo.mapDem.get(v).iterator();
                    while (dItr.hasNext()) {
                        Object entry2 = dItr.next();
                        Demand dReq = (Demand) entry2;
                        String boxDem = dReq.getBoxSize();
                        int b = fInfo.mapBx2.get(boxDem);
                        bunchPerBox.addTerm(w[v][b][t], 1.0);  //sum(over b)w[v][b][t]                        
                    }
                    if (t == 0) {
                        //H2[v][0]+u[v][t]/s[v]-sum(over b)w[v][b][t] = h2[v][t]
                        cplex.addEq(cplex.sum(inflowBunch, cplex.prod(u[v][t], 1.0 / stems2bunch)),
                                cplex.sum(bunchPerBox, h2[v][t]),
                                "H2 (" + v + ")(" + t + ")");
                    } else {
                        //h2[v][t-1]+H2[v][t]+u[v][t]/s[v]-sum(over b)w[v][b][t] = h2[v][t]
                        cplex.addEq(cplex.sum(cplex.sum(h2[v][t - 1], inflowBunch),
                                cplex.prod(u[v][t], 1.0 / stems2bunch)),
                                cplex.sum(h2[v][t], bunchPerBox),
                                "h2 (" + v + ")(" + t + ")");
                    }
                    bunchPerBox.clear();

                    for (int b = 0; b < boxSize; b++) {
                        String boxS = fInfo.mapBx1.get(b);
                        String longKeyConv = fInfo.mapP2.get(v) + boxS + fInfo.mapP.get(v).getLength();
                        Conversion convData2 = fInfo.mapCv2.get(longKeyConv);
                        int bunch2box = convData2.getBunchBox();
                        Iterator dItr2 = fInfo.mapDem.get(v).iterator();
                        while (dItr2.hasNext()) {
                            Object entry3 = dItr2.next();
                            Demand dReq2 = (Demand) entry3;
                            String boxDem = dReq2.getBoxSize();
                            int bx = fInfo.mapBx2.get(boxDem);
                            int poIDk = dReq2.getPoId();
                            int k = fInfo.mapPOint.get(poIDk);
                            if (bx == b) {
                                boxToBatch.addTerm(a[v][bx][k][t], 1.0); //sum(over k)a[v][b][k][t]
                            }
                        }
                        if (t == 0) {
                            //H3[v][b][t]+w[v][b][t]/M[v][b]
                            //-sum(over k)a[v][b][k][t] = h3[v][b][t]
                            cplex.addEq(cplex.sum(inflowBox, cplex.prod(w[v][b][t], 1.0 / bunch2box)),
                                    cplex.sum(boxToBatch, h3[v][b][t]),
                                    "H3 (" + v + ")(" + b + ")(" + t + ")");
                        } else {
                            //H3[v]+h3[v][b][t-1]+w[v][b][t]/M[v][b]
                            //-sum(over k)a[v][b][k][t] = h3[v][b][t]
                            cplex.addEq(cplex.sum(cplex.sum(inflowBox, h3[v][b][t - 1]),
                                    cplex.prod(w[v][b][t], 1.0 / bunch2box)),
                                    cplex.sum(h3[v][b][t], boxToBatch),
                                    "h3 (" + v + ")(" + b + ")(" + t + ")");
                        }
                        boxToBatch.clear();
                    }
                }
            }

            endTime = System.currentTimeMillis();
            long timeSpend2 = (endTime - startingTime) / 1000;

            startingTime = System.currentTimeMillis();

            //CAPACITY AT FIXING BUNCHES        sum(over v)u[v][t]<=q1_[v]  forall t
            //CAPACITY AT FIXING BOXES          sum(over v,b)w[v][b][t]<=q2  forall t
            IloLinearNumExpr bunchCapacityR = cplex.linearNumExpr();
            IloLinearNumExpr bunchCapacityC = cplex.linearNumExpr();
            IloLinearNumExpr bunchCapacityMC = cplex.linearNumExpr();
            IloLinearNumExpr boxCapacity = cplex.linearNumExpr();
            //Bunch Capacity
            for (int t = 0; t < timeSize; t++) {// forall t
                //sum(over v)u[v][t]  
                for (Demand vElement : fInfo.flowerDemand) {  //flowerDemand.add(demandKey)
                    int v = vElement.getKeyProduct();
                    if (fInfo.mapP2.containsKey(v)) { //identify Product (rose, carn...) from variety
                        String product = fInfo.mapP2.get(v);
                        switch (product) {
                            case "Rose":
                                bunchCapacityR.addTerm(u[v][t], 1.0);
                                //System.out.println("bunches addend to rose line capacity");
                                break;
                            case "Carnation":
                                bunchCapacityC.addTerm(u[v][t], 1.0);
                                //System.out.println("bunches addend to carnation line capacity");
                                break;
                            case "Minicarnation":
                                bunchCapacityMC.addTerm(u[v][t], 1.0);
                                //System.out.println("bunches addend to minicarnation line capacity");
                                break;
                        }
                    }

                }// Close sum over variety loop
                cplex.addLe(bunchCapacityR, q1_rs, "roseCapacity");
                cplex.addLe(bunchCapacityC, q1_rc, "carnationCapacity");
                cplex.addLe(bunchCapacityMC, q1_rmc, "miniCapacity");
                bunchCapacityR.clear();
                bunchCapacityC.clear();
                bunchCapacityMC.clear();
            }// Close for all time periods loop

            //Box Capacity
            for (int t = 0; t < timeSize; t++) {// forall t
                //sum(over v)w[v][b][t]
                for (Demand vElement : fInfo.flowerDemand) {  //flowerDemand.add(demandKey)
                    int v = vElement.getKeyProduct();
                    String boxDem = vElement.getBoxSize();
                    //sum(over b)w[v][b][t]
                    for (int b = 0; b < boxSize; b++) { //forall boxes
                        String boxS = fInfo.mapBx1.get(b); //from int at loop to String listed in flowerDemand
                        if (boxS.equals(boxDem)) {
                            boxCapacity.addTerm(w[v][b][t], 1.0);
                        }
                    }//close sum over boxes loop
                }//close sum over varieties loop
                cplex.addLe(boxCapacity, q2_s, "boxCapacity");
                boxCapacity.clear();
            }//close for all time periods loop

            endTime = System.currentTimeMillis();
            long timeSpend3 = (endTime - startingTime) / 1000;

            startingTime = System.currentTimeMillis();
            //System.out.println("MATCHING BATCHES TO DEMAND a[v][b][k][t]=x[k][t]*D[v][b][k]");
            for (Map.Entry<Integer, Set<Demand>> demandR : fInfo.mapDem.entrySet()) {
                int v = demandR.getKey();
                for (int t = 0; t < timeSize; t++) {
                    double demandQty;
                    Iterator dItr = demandR.getValue().iterator();
                    while (dItr.hasNext()) {  // per variety search at demand each request
                        Object entry = dItr.next();
                        Demand vDemand = (Demand) entry;
                        int poID = vDemand.getPoId();  //Number of client eg. 2459227
                        int k = fInfo.mapPOint.get(poID);  //PO number 0 to poSize
                        Set<Demand> poIDSet = fInfo.mapPO.get(poID);
                        Iterator pItr = poIDSet.iterator();
                        while (pItr.hasNext()) {
                            Object entry2 = pItr.next();
                            Demand pDemand = (Demand) entry2;
                            int var = pDemand.getKeyProduct();
                            if (v == var) {
                                String boxS = pDemand.getBoxSize();
                                int b = fInfo.mapBx2.get(boxS);
                                demandQty = pDemand.getPoBoxQty();
                                cplex.addEq(a[v][b][k][t], cplex.prod(x[k][t], demandQty),
                                        "a(" + v + ")(" + b + ")(" + k + ")(" + t + ")");
                            }
                        }
                    }
                }
            }

            endTime = System.currentTimeMillis();
            long timeSpend4 = (endTime - startingTime) / 1000;
            long timeSpend5 = (endTime - startingTimeModel) / 1000;

            if (printTimeSpend) {
                System.out.println("\nVariables were created in "
                        + timeSpend1 + " seconds.");
                System.out.println("Inventory constraints were created in "
                        + timeSpend2 + " seconds");
                System.out.println("Capacity constraints were created in "
                        + timeSpend3 + " seconds");
                System.out.println("Creating batches to demand variables: "
                        + timeSpend4 + " seconds");
                System.out.println("Total Model generation before solving, spent: "
                        + timeSpend5 + " seconds\n");//*/
            }

            if (cplex.solve()) {
                status = printStatus(cplex);
                if (mIP == 1 && status == 1) {
                    printResults(cplex, poSize, timeSize, x, y);
                }
            } else {
                System.out.println("\nStatus: " + cplex.getCplexStatus());
                status = 0;
            }

            if (mIP == 1 && !fInfo.runAll && status == 1) {

                System.out.print("\nPrint MIP Pattern to paste for LNS? Y/N\t");
                String printEval4LNS = input.next();
                if (printEval4LNS.equalsIgnoreCase("Y")) {
                    evaluationLNS(cplex, poSize, timeSize, x, y);
                }
                System.out.print("\nPrint all variables?  Y/N\t");
                String printAllVar = input.next();
                if (printAllVar.equalsIgnoreCase("Y")) {
                    printAllVariables(cplex, poSize, timeSize,
                            boxSize, q1_rs, q1_rc, q1_rmc, h1, h2, u, w, h3, a, x, y);
                }
            }

            cplex.end();
            return status;
        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
            status = 0;
            return status;
        }
    }

    int printStatus(IloCplex cplex) {
        try {
            System.out.println("PRINT STATUS");
            System.out.println("Solution Status: " + cplex.getStatus());
            int objVal = (int) (cplex.getObjValue());
            System.out.println("Objective: " + objVal + "\t");
            status = 1;
            return status;
        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
            status = 0;
            return status;
        }
    }

    void printResults(IloCplex cplex, int poSize, int timeSize, IloNumVar[][] x,
            IloNumVar[] y) {
        try {
            System.out.println("\nPrint MIP result for x[k][t] and y[k]\n");
            int acumPty = 0;
            int[] distriTime = new int[fInfo.periodsUsed];
            for (int k = 0; k < poSize; k++) {     //sum(over k)
                if (cplex.getValue(y[k]) > 0) {
                    acumPty = acumPty + fInfo.bigPty;
                    valY[k] = 1;
                    //System.out.print(cplex.getValue(y[k]) + ": ");
                    System.out.println("y(" + k + ")\t" + fInfo.bigPty
                            + "\t" + acumPty);
                } else {
                    for (int t = 0; t < timeSize; t++) {
                        if (cplex.getValue(x[k][t]) > 0) {
                            valX[k][t] = 1;
                            distriTime[t] = distriTime[t] + 1;
                            acumPty = acumPty + fInfo.tPty[k][t];
                            //System.out.print(cplex.getValue(x[k][t]) + ": ");
                            System.out.println("x(" + k + "," + t + ") =\t "
                                    + fInfo.tPty[k][t] + "\t" + acumPty);
                            break;
                        }
                    }
                }
            }

            System.out.println("\nPrint MIP result: time sequence\n");

            for (int t = 0; t < timeSize; t++) {
                for (int k = 0; k < poSize; k++) {
                    if (cplex.getValue(x[k][t]) > 0) {
                        System.out.print("x(" + k + "," + t + ") = "
                                + fInfo.tPty[k][t]);
                        for (Map.Entry<Integer, Integer> entryTP : fInfo.mapPOint.entrySet()) {
                            int foundVal = 0;
                            int key = entryTP.getKey();
                            int value = entryTP.getValue();
                            for (Map.Entry<Integer, Integer> entryTP2 : fInfo.mapEarlyTP.entrySet()) {
                                int key2 = entryTP2.getKey();
                                int value2 = entryTP2.getValue();
                                if (k == value && key == key2) {
                                    //System.out.println();
                                    System.out.println("\t & etp was: " + value2);
                                    foundVal = 1;
                                    break;
                                }
                            }
                            if (foundVal == 1) {
                                break;
                            }
                        }
                    }
                }
            }
            for (int k = 0; k < poSize; k++) {
                if (cplex.getValue(y[k]) > 0) {
                    System.out.println("y(" + k + ")\tPty = " + fInfo.bigPty);
                }
            }
            System.out.print("\nDistribution:\t");
            int maxDist = 0;
            int cntD = 0;
            for (int t = 0; t < fInfo.periodsUsed; t++) {
                cntD++;
                if (cntD < 23) {
                    System.out.print(" " + distriTime[t] + " ");
                } else {
                    System.out.print(" " + distriTime[t] + "\n");
                    cntD = 0;
                }
                maxDist = Math.max(maxDist, distriTime[t]);
            }
            System.out.print(" MaxD = " + maxDist + "\n");
        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
            status = 0;
        }
    }

    void evaluationLNS(IloCplex cplex, int poSize, int timeSize, IloNumVar[][] x, IloNumVar[] y) {
        try {
            System.out.println("\nPaste at LNS for evaluation\n");
            for (int k = 0; k < poSize; k++) {     //sum(over k)
                if (cplex.getValue(y[k]) == 1) {
                    System.out.println("y_var[" + k + "] = 1;");
                } else {
                    for (int t = 0; t < timeSize; t++) {
                        if (cplex.getValue(x[k][t]) == 1) {
                            System.out.println("x_var[" + k + "][" + t + "]=1;");
                            break;
                        }
                    }
                }
            }
        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
            status = 0;
        }
    }

    void printAllVariables(IloCplex cplex, int poSize, int timeSize,
            int boxSize, int q1_rs, int q1_rc, int q1_rmc,
            IloNumVar[][] h1, IloNumVar[][] h2, IloNumVar[][] u, IloNumVar[][][] w,
            IloNumVar[][][] h3, IloNumVar[][][][] a, IloNumVar[][] x, IloNumVar[] y) {
        try {
            System.out.println("\nInventory\n");
            for (int t = 0; t < timeSize; t++) {
                System.out.println("\nTime period " + t);
                //int qtyBoxing = 0;
                int qtyRAcc = 0;
                int qtyCAcc = 0;
                int qtyMCAcc = 0;
                for (Map.Entry<Integer, Set<Demand>> varReq : fInfo.mapDem.entrySet()) { //forall variety in demand map
                    int v = varReq.getKey(); //variety key
                    String product = fInfo.mapP2.get(v);
                    System.out.println();
                    Iterator itr = fInfo.offerInflowReq.get(v).iterator();
                    while (itr.hasNext()) {
                        Object entry = itr.next();
                        Offer offerProd = (Offer) entry;
                        String unitOR = offerProd.getUnit();
                        int qtyOR = offerProd.getQty();
                        int inflowTime = offerProd.getTime();
                        if (t == inflowTime) {
                            System.out.print("Offer: " + qtyOR + " " + unitOR
                                    + " of " + v + "\t at time period "
                                    + inflowTime + " \nrequired by {");

                            for (Demand vElement : fInfo.flowerDemand) {
                                int vD = vElement.getKeyProduct();
                                int poID = vElement.getPoId();
                                int qtyStems = vElement.getPoStemsQty();
                                for (Map.Entry<Integer, Integer> kElement : fInfo.mapPO2.entrySet()) {
                                    int poInt = kElement.getKey();
                                    int poID2 = kElement.getValue();
                                    if (v == vD && poID == poID2) {
                                        switch (product) {
                                            case "Rose":
                                                qtyRAcc = qtyRAcc + qtyStems;
                                                break;
                                            case "Carnation":
                                                qtyCAcc = qtyCAcc + qtyStems;
                                                break;
                                            case "Minicarnation":
                                                qtyMCAcc = qtyMCAcc + qtyStems;
                                                break;
                                        }
                                        System.out.print("k=" + poInt
                                                + " (" + qtyStems + "), ");
                                    }
                                }
                            }
                            System.out.print("}: \nReq \tR = " + qtyRAcc
                                    + " \tC = " + qtyCAcc + " \tMC = " + qtyMCAcc);
                            System.out.println("\nStem_C \tR = " + q1_rs
                                    + " \tC = " + q1_rc + " \tMC = " + q1_rmc);
                        }
                    }

                    double stemInv = cplex.getValue(h1[v][t]);
                    double bunchInv = cplex.getValue(h2[v][t]);
                    double bunchFix = cplex.getValue(u[v][t]);
                    if (stemInv != 0.0 || bunchInv != 0.0 || bunchFix != 0.0) {
                        if (stemInv != 0.0) {
                            System.out.println("h1[" + v + "," + t + "] = "
                                    + stemInv + " stems stored");
                        }
                        if (bunchInv != 0.0) {
                            System.out.println("h2[" + v + "," + t + "] = "
                                    + bunchInv + " bunches stored");
                        }
                        if (bunchFix != 0.0) {
                            System.out.println("u[" + v + "," + t + "] = "
                                    + bunchFix + " stems bunched");
                        }
                    }

                    for (int b = 0; b < boxSize; b++) { //forall boxes 
                        String boxS = fInfo.mapBx1.get(b); //from int at loop to String listed in flowerDemand
                        double boxInv = cplex.getValue(h3[v][b][t]);
                        double boxFix = cplex.getValue(w[v][b][t]);

                        if (boxInv != 0.0 || boxFix != 0.0) {
                            if (boxFix != 0.0) {
                                System.out.println("w[" + v + ","
                                        + b + "," + t + "]= "
                                        + boxFix + " bunches boxed");
                            }
                            if (boxInv != 0.0) {
                                System.out.println("h3[" + v + ","
                                        + b + "," + t + "]= "
                                        + boxInv + " boxes stored");
                            }
                        }
                        for (int k = 0; k < poSize; k++) { // forall clients
                            int poId = fInfo.mapPO2.get(k); //from int at loop k to POid listed in flowerDemand
                            for (Demand vElement : fInfo.flowerDemand) {  //flowerDemand.add(demandKey)
                                int vD = vElement.getKeyProduct();
                                String boxDem = vElement.getBoxSize();
                                int poDem = vElement.getPoId();
                                if (v == vD && boxS.equals(boxDem)
                                        && poId == poDem) {
                                    double batchFix = cplex.getValue(a[v][b][k][t]);
                                    if (batchFix != 0.0) {
                                        System.out.println(
                                                "a["
                                                + v + "," + b
                                                + "," + k + ","
                                                + t + "]= "
                                                + batchFix + " boxes batched");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IloException e) {
            System.err.println("Concert exception caught: " + e);
            status = 0;
        }
    }
}
