/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package packingoperation;

import java.io.IOException;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Map;
import ilog.concert.*;
//import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author andresposadac
 */
public class PackingOperation extends FlowerData {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws ilog.concert.IloException
     */
    public static void main(String[] args) throws IOException, IloException {
        Map<Integer, Integer> mapOF = new TreeMap<>();
        Map<Integer, Long> mapOF2 = new TreeMap<>();
        Map<Integer, Integer> mapServedK = new TreeMap<>();
        Scanner input = new Scanner(System.in);
        double mipGap = 0.0;
        FlowerData param = new FlowerData();  //param object to retrieve Data info 
        Lns pattern = new Lns(param);  //pattern object to generate different schedule patterns
        System.out.print("\nSolve all with LNS? Y/N:\t");
        String allInstances = input.next();
        boolean solveAll = false;
        int iterAll = 1, instances, lnsTest = 1;
        Set<Integer> instanceSet = new TreeSet<>();
        if (allInstances.equalsIgnoreCase("Y")) {
            solveAll = true;
            System.out.print("\nHow many instances?\t");
            instances = input.nextInt();

            instanceSet.clear();
            System.out.print("\nChoose instances of 20, 30, 50, 70, 80, 100");
            for (int i = 0; i < instances; i++) {
                System.out.print("\nInstance of\t");
                instanceSet.add(input.nextInt());
            }
            System.out.print("\nHow many to Test with LNS?\t");
            lnsTest = input.nextInt();
            iterAll = (instances * lnsTest) + instances; //(LNS test) + MIP
            System.out.println("");
        }
        int cntItr = 0;
        int instance = 20;
        Set<Integer> instanceCheckSet = new TreeSet<>();
        instanceCheckSet.clear();
        for (int itAll = 0; itAll < iterAll; itAll++) {
            cntItr++;
            if (cntItr <= 1) {
                for (int inst : instanceSet) {
                    if (!instanceCheckSet.contains(inst)) {
                        instanceCheckSet.add(inst);
                        instance = inst;
                        break;
                    }
                }
                if (!param.askParameters(solveAll, instance)) {
                    System.out.println("File error!");
                    break;
                }
            }

            int itr = 0, incum = 0, lastItr = 0, fcnt = 0, nfcnt = 0;

            if (!param.getFeasibility()) {
                System.out.println("PRESOLVE INFEASIBLE");
            } else {
                System.out.println("PRESOLVE FEASIBLE");
                param.earlierTimePeriodPO();
                int perform, iterations = 1;
                String printTrace = "N";
                if (solveAll) {
                    if (cntItr == 1) {                        
                        perform = 1;
                        mipGap = 0.07;
                    } else {
                        perform = 0;
                        mipGap = 0.95;
                        if (instance <= 100) {
                            iterations = 250;
                        } else if (instance <= 250) {
                            iterations = 300;
                        } else {
                            iterations = 450;
                        }
                        printTrace = "N";
                    }
                } else {
                    param.printMapsNsets();
                    System.out.print("\nPerform with: type for MIP = 1 or LNS = 0 : ");
                    perform = input.nextInt();
                    if (perform == 0) {
                        mipGap = 0.95;
                        System.out.print("\nHow many iterations to perform? : ");
                        iterations = input.nextInt();
                        System.out.print("Whant to print LNS trace? Y/N:\t");
                        printTrace = input.next();
                    } else {
                        System.out.print("MIP GAP (eg 5% type 0.05) = ");
                        mipGap = input.nextDouble();
                    }
                }
                JobFlowModel check = new JobFlowModel(param, pattern);
                pattern.performMethod(perform);
                pattern.mipGap(mipGap);
                if (perform == 0) {
                    int totalClients = pattern.createVar();
                    pattern.trivialPattern();   //number of clients
                    pattern.incumbentPattern();
                    int leftClients = totalClients;
                    System.out.println("\nStart finding best solution with LNS for "
                            + leftClients + " clients\n**** Starting LNS ****");
                    long startTimeLNS = System.currentTimeMillis();
                    int objFv = pattern.detProb();
                    mapOF.clear();
                    mapOF2.clear();
                    mapServedK.clear();
                    if (check.packingFlow(perform, mipGap) == 1) {
                        fcnt++;
                        incum++;
                        objFv = pattern.incumbentPattern();
                        mapOF.put(incum, objFv);
                        long endTime = System.currentTimeMillis();
                        long incumTime = evaluateTime(startTimeLNS, startTimeLNS, endTime);
                        mapOF2.put(incum, incumTime);
                        int sK = pattern.servedK.size();
                        mapServedK.put(incum, sK);
                    }
                    int notImproved = 0;
                    for (int i = 0; i < iterations; i++) {
                        boolean fCheck = true;
                        int notIncumbent = 0;
                        int methodType = 1; // 1 = aggressive, 2 = medium, 3 = moderate
                        itr = i + 1;

                        while (fCheck) {
                            pattern.detProb();
                            int repaired = repairMethod(pattern, i,
                                    printTrace, objFv, methodType);
                            if (repaired < objFv) {
                                System.out.println("\nPattern repaired, evaluate:\t"
                                        + repaired + " < "
                                        + objFv + "\t(Check with LP Model)\n");

                                long startTime = System.currentTimeMillis();
                                int modelStatus = 0;

                                if (check.packingFlow(perform, mipGap) == 1) {
                                    methodType = 1;
                                    modelStatus = 1;
                                    notIncumbent = 0;
                                    notImproved = 0;
                                    fcnt++;
                                    incum++;
                                    objFv = pattern.incumbentPattern();
                                    mapOF.put(incum, objFv);
                                    lastItr = i;
                                    long endTime = System.currentTimeMillis();
                                    long incumTime = evaluateTime(startTimeLNS, startTime, endTime);
                                    mapOF2.put(incum, incumTime);
                                    int sK = pattern.servedK.size();
                                    mapServedK.put(incum, sK);
                                } else {
                                    long endTime = System.currentTimeMillis();
                                    evaluateTime(startTimeLNS, startTime, endTime);
                                    pattern.detProb();
                                    notIncumbent++;
                                    notImproved++;
                                    nfcnt++;
                                    if (pattern.notServedK.isEmpty()) {
                                        break;
                                    } else if (notIncumbent > 8) {
                                        System.out.println("\t** Use Destroy Method **");
                                        break;
                                    } else if (notIncumbent < 5) {
                                        methodType = 2;
                                    } else {
                                        methodType = 3;
                                    }
                                    pattern.refreshIncumbent();
                                    destroyMethod(pattern, i,
                                            printTrace, objFv, methodType);
                                }
                                showStatus(pattern, modelStatus, repaired,
                                        objFv, i, iterations, notIncumbent);
                            } else {
                                // Objective Funcions was not improved by repair operator
                                methodType = 1;
                                notImproved++;
                                fCheck = false; //exit loop
                            }
                        }  //end while loop
                        pattern.refreshIncumbent();
                        if (pattern.notServedK.isEmpty()
                                && notImproved > iterations * 0.25) {
                            pattern.printIncumbent();
                            notImproved = 0;
                            //break;
                        } else {
                            methodType = 1;
                            destroyMethod(pattern, i,
                                    printTrace, objFv, methodType);
                        }
                    } //end of iterations

                    pattern.refreshIncumbent();
                    printPartialResults(pattern, lastItr, itr, fcnt, nfcnt,
                            mapOF, mapOF2, mapServedK, startTimeLNS);

                    long endTimeLNS = System.currentTimeMillis();
                    printFile(param, pattern, check, itAll, perform, mapOF, mapOF2,
                            mapServedK, startTimeLNS, startTimeLNS, endTimeLNS);
                    /*
                     perform = 1;
                     System.out.println("\nCompared with MIP: ");
                     check.packingFlow(perform, mipGap);//*/
                } else {
                    long startTimeMIP = System.currentTimeMillis();
                    check.packingFlow(perform, mipGap);
                    //pattern.mipGap(check.getGap());
                    long endTimeMIP = System.currentTimeMillis();
                    evaluateTime(startTimeMIP, startTimeMIP, endTimeMIP);
                    printFile(param, pattern, check, itAll, perform, mapOF, mapOF2,
                            mapServedK, startTimeMIP, startTimeMIP, endTimeMIP);
                }
            }
            if (cntItr == lnsTest + 1) {
                cntItr = 0;
            }
        }
    }

    static void showStatus(Lns pattern, int modelStatus, int repaired,
            int objFv, int i, int iterations, int notIncumbent) {
        String modelStatusS;
        if (modelStatus == 0) {
            modelStatusS = "Infeasible";
        } else {
            modelStatusS = "Feasible";
        }
        System.out.println("\nTested for OF= "
                + repaired + " & incumbent= " + objFv
                + "\t\t>> Status: " + modelStatusS
                + "\n@Iteration " + i + "/" + iterations
                + "\t\t\t\t\tOF not change: " + notIncumbent);
        pattern.printDistribution();
        System.out.println("\n>> (" + pattern.servedK.size() + " out of "
                + pattern.clients + " clients) << ");
        if (pattern.notServedK.size() < 10) {
            System.out.print("\nNot Served PO: {");
            for (Integer c : pattern.notServedK) {
                System.out.print(" " + c);
            }
            System.out.println("}");
        }
        System.out.println("************************************");

    }

    static int repairMethod(Lns pattern, int i, String printTrace, int objFv,
            int methodType) {

        String operatorType = "repair";
        double nsP = pattern.lnsGap("repair");
        double sP = pattern.lnsGap("destroy");
        boolean oneByOne = false;
        int repaired = pattern.objF_call();
        //int totalClients = pattern.clients;
        int chooseRO; //choose repair operator to use

        if (printTrace.equalsIgnoreCase("Y")) {
            System.out.println("\nAt iteration " + i
                    + " : Repair\tnow Obj = " + repaired);
        }

        double rNprob = Math.random();
        double repBlock = selectProb(pattern, operatorType, methodType);
        if (methodType > 1) {
            oneByOne = true;
        }
        double probOb = repBlock;
        int objFc = (int) Math.round(objFv * (1.0 - probOb));
        //System.out.println("REPAIR Do while " + repaired + " > " + objFc);
        while (repaired >= objFc) {

            if (nsP > sP) {
                chooseRO = (int) Math.rint(Math.random() * 5);
            } else if (sP > 0.3) {
                chooseRO = (int) Math.rint(Math.random() * 3);
            } else {
                chooseRO = 3;
            }

            if (pattern.notServedK.isEmpty()) {
                break;
            }
            repaired = pattern.repairOperator(chooseRO);
            if (printTrace.equalsIgnoreCase("Y")) {
                //System.out.print(stopWhile + ") ");
                pattern.printTrace(operatorType);
            }
        }
        System.out.print("\nRepair PO = {");
        for (Integer c : pattern.repairedPO) {
            System.out.print(" " + c);
        }
        System.out.println(" }\t Now OF = " + repaired);
        pattern.clearTempSets();

        return repaired;
    }

    static void destroyMethod(Lns pattern, int i,
            String printTrace, int objFv, int methodType) {

        String operatorType = "destroy";
        double nsP = pattern.lnsGap("repair");
        double sP = pattern.lnsGap("destroy");
        int destroyed = pattern.objF_call();
        if (printTrace.equalsIgnoreCase("Y")) {
            System.out.println("\nAt iteration " + i
                    + " : Destroy\tnow Obj = " + destroyed);
        }
        double repBlock = selectProb(pattern, operatorType, methodType);

        int maxTotalPty = pattern.highestPty();
        int objFd = (int) (objFv + Math.rint((maxTotalPty - objFv) * repBlock));

        while (destroyed <= objFd) {
            int distOperType;
            if (nsP > sP) {
                distOperType = (int) Math.rint(Math.random() * 3);
                if (distOperType == 0) {
                    distOperType = 1;
                }
            } else {
                distOperType = 2;
            }

            if (pattern.servedK.isEmpty()) {
                break;
            }

            destroyed = pattern.destroyOperatorRev(distOperType);

            if (printTrace.equalsIgnoreCase("Y")) {
                pattern.printTrace(operatorType);
            }
        }

        System.out.print("\nDestroy PO = {");
        for (Integer c : pattern.destroyedPO) {
            System.out.print(" " + c);
        }
        System.out.println(" }\t Now OF = " + destroyed);
        pattern.clearTempSets();
    }

    static double selectProb(Lns pattern, String operatorType,
            int methodType) {
        double probR = Math.random();
        double mtProb = Math.random();
        double nsP = pattern.lnsGap("repair");
        double sP = pattern.lnsGap("destroy");
        switch (methodType) {
            case 1:
                mtProb = mtProb * 0.8; // aggressive
                break;
            case 2:
                mtProb = mtProb * 0.35; // medium
                break;
            case 3:
                mtProb = mtProb * 0.1; // moderate
                break;
        }

        switch (operatorType) {
            case "repair":
                probR = (nsP * mtProb); // big at first, small in the end
                break;
            case "destroy":
                probR = (sP * mtProb); // small at first, big in the end
                break;
        }

        return probR;
    }

    static long evaluateTime(long startTimeLNS, long startTime, long endTime) {
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
        System.out.println("Time solvling " + totalTime + " seconds");
        switch (whatToPrint) {
            case 1:
                System.out.println("Elapsed Time " + totalTime_secLeft + " seconds.");
                break;
            case 2:
                System.out.println("Elapsed Time " + totalTime_minLeft
                        + " minutes and " + totalTime_secLeft + " seconds.");
                break;
            case 3:
                System.out.println("Elapsed Time " + totalTime_hours + " hours, "
                        + totalTime_minLeft + " minutes, "
                        + totalTime_secLeft + " seconds.");
                break;
        }
        return totalTimeAcc;
    }

    static void printPartialResults(Lns pattern,
            int lastItr, int itr, int fcnt, int nfcnt,
            Map<Integer, Integer> mapOF, Map<Integer, Long> mapOF2,
            Map<Integer, Integer> mapServedK, long startTimeLNS) throws IOException {

        System.out.println("\n\nAt iteration # " + lastItr
                + " found incumbent. After " + itr + " iterations.");
        //pattern.refreshIncumbent();
        pattern.detProb();
        pattern.printPattern();
        pattern.printPatternByt();
        System.out.println("\nLNS:\n # \tPenalty");
        for (Map.Entry<Integer, Integer> resultsLNS : mapOF.entrySet()) {
            int iter = resultsLNS.getKey();
            int objFval = resultsLNS.getValue();
            long secReg = mapOF2.get(iter);
            int servedClients = mapServedK.get(iter);
            int hourReg_0 = (int) (secReg / 3600);
            int minReg_0 = (int) (secReg / 60) - (hourReg_0 * 60);
            int secReg_0 = (int) secReg - (minReg_0 * 60);
            System.out.print(iter + "\t" + objFval + "\tat (" + hourReg_0
                    + ":" + minReg_0 + ":" + secReg_0 + ")\t("
                    + servedClients + "/" + pattern.clients + ")\n");
        }
        System.out.println("\nFound " + fcnt
                + " feasible patterns and rejected "
                + nfcnt + " for infeasibility \n");

        long endTimeLNS = System.currentTimeMillis();
        evaluateTime(startTimeLNS, startTimeLNS, endTimeLNS);
    }

    static void printFile(FlowerData param, Lns pattern,
            JobFlowModel check, int itAll, int perform,
            Map<Integer, Integer> mapOF, Map<Integer, Long> mapOF2,
            Map<Integer, Integer> mapServedK,
            long startTimeLNS, long startTime, long endTime) throws IOException {
        String fNum = param.getInstNum(itAll);
        String fileName = "/users/andresposadac/Dropbox/LnsFlowersData/flowerResults/flowerSolution"
                + fNum + ".txt";
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        if (perform == 1) {
            pattern.createVar();
            pattern.trivialPattern();
            for (int c = 0; c < param.getPOnumber(); c++) {
                if (check.valY[c] == 1) {
                    pattern.assignValues("y", c, 0);
                } else {
                    for (int t = 0; t < param.getPeriods(); t++) {
                        if (check.valX[c][t] == 1) {
                            pattern.assignValues("x", c, t);
                        }
                    }
                }
            }
            pattern.printPatternFinal(fw, perform, mapOF, mapOF2, mapServedK,
                    startTimeLNS, startTime, endTime);
        } else {
            pattern.printPatternFinal(fw, perform, mapOF, mapOF2, mapServedK,
                    startTimeLNS, startTime, endTime);
        }
        System.out.println("Done");
    }
}
