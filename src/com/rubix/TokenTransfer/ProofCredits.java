package com.rubix.TokenTransfer;


import com.rubix.Consensus.InitiatorConsensus;
import com.rubix.Consensus.InitiatorProcedure;
import com.rubix.Resources.Functions;
import com.rubix.Resources.IPFSNetwork;
import io.ipfs.api.IPFS;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import static com.rubix.Resources.Functions.*;


public class ProofCredits {


    public static Logger ProofCreditsLogger = Logger.getLogger(ProofCredits.class);
    private static ArrayList quorumPeersList;
    private static ArrayList alphaPeersList;
    private static ArrayList betaPeersList;
    private static ArrayList gammaPeersList;

    public static JSONObject create(String data, IPFS ipfs) throws IOException, JSONException {

        JSONObject APIResponse = new JSONObject();
        JSONObject detailsObject = new JSONObject(data);
        String receiverDidIpfsHash = detailsObject.getString("receiverDidIpfsHash");
        String pvt = detailsObject.getString("pvt");
        int creditUsed=0;
        long totalTime=0;

        JSONArray alphaQuorum = null;
        JSONArray betaQuorum=null;
        JSONArray gammaQuorum=null;



        // getInfo api call to fetch current token, current level and required proof credits for level


//
//        int level = 0,tokenNumber = 0,availableCredits = 0, balance;
//        long starttime = System.currentTimeMillis();
//        String GET_URL = SYNC_IP+"/getInfo";
//        URL URLobj = new URL(GET_URL);
//        HttpURLConnection con = (HttpURLConnection) URLobj.openConnection();
//        con.setRequestMethod("GET");
//        int responseCode = con.getResponseCode();
//        System.out.println("GET Response Code :: " + responseCode);
//        if (responseCode == HttpURLConnection.HTTP_OK) {
//            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//            String inputLine;
//            StringBuffer response = new StringBuffer();
//            while ((inputLine = in.readLine()) != null) {
//                response.append(inputLine);
//            }
//            in.close();
//            ProofCreditsLogger.debug("response from service "+response.toString());
//
//            JSONObject resJsonData = new JSONObject(response.toString());
//            level = resJsonData.getInt("current_level");
//            tokenNumber = resJsonData.getInt("token_num");
//            availableCredits = resJsonData.getInt("required_credits");
//            ProofCreditsLogger.debug("level "+level+" tokenNumber "+tokenNumber+" required proof credits "+availableCredits);
//
//        } else
//            ProofCreditsLogger.debug("GET request not worked");
//
//
//
//        //Reading proofcredits.json
//        String jsonFilePath = WALLET_DATA_PATH+"QuorumSignedTransactions.json";
//        JSONArray records = new JSONArray(readFile(jsonFilePath));
//        balance = records.length();
//        JSONArray prooftid = new JSONArray();
//        for (int i = 0; i < availableCredits; i++) {
//            JSONObject temp = records.getJSONObject(i);
//            if(temp.getBoolean("minestatus")==false) {
//                prooftid.put(temp.getString("tid"));
//                records.getJSONObject(i).put("minestatus",true);
//            }
//        }
//
//        balance -= prooftid.length();
//
//        ProofCreditsLogger.debug("Current balance of node : " + balance);
//
//        //Check if node can mine token
//        if (balance >= availableCredits) {
//            //Calling Mine token function
//
//            String token = Functions.mineToken(level, tokenNumber);
//
//
//            String comments = level+String.valueOf(tokenNumber)+prooftid;
//
//
//            String authSenderByRecHash = calculateHash(token + receiverDidIpfsHash + comments, "SHA3-256");
//            String tid = calculateHash(authSenderByRecHash, "SHA3-256");
//            JSONArray quorumArray = new JSONArray(readFile(DATA_PATH + "quorumlist.json"));
//
//            quorumPeersList = QuorumCheck(quorumArray, ipfs);
//
//
//            JSONObject dataObject = new JSONObject();
//            dataObject.put("tid", tid);
//            dataObject.put("message", comments);
//            dataObject.put("receiverDidIpfs", receiverDidIpfsHash);
//            dataObject.put("pvt", pvt);
//            dataObject.put("senderDidIpfs", receiverDidIpfsHash);
//            dataObject.put("token", token);
//            dataObject.put("alphaList", quorumPeersList.subList(0, 7));
//            dataObject.put("betaList", quorumPeersList.subList(7, 14));
//            dataObject.put("gammaList", quorumPeersList.subList(14, 21));
//
//            InitiatorProcedure.consensusSetUp(dataObject.toString(), ipfs, SEND_PORT + 3);
//
//            if (!(InitiatorConsensus.quorumSignature.length() >= 3 * minQuorum(7)))
//            {
//            APIResponse.put("did", receiverDidIpfsHash);
//            APIResponse.put("tid", "null");
//            APIResponse.put("status", "Failed");
//            APIResponse.put("message", "Consensus failed");
//            ProofCreditsLogger.debug("consensus failed");
//            }
//            else
//            {
//            ProofCreditsLogger.debug("token mined " + token);
//            //call updateTxnAPI
//            String GET_URL2 = SYNC_IP + "/updateTxn";
//            URL URLobj2 = new URL(GET_URL2);
//            HttpURLConnection con2 = (HttpURLConnection) URLobj2.openConnection();
//            con2.setRequestMethod("GET");
//            int responseCode2 = con2.getResponseCode();
//            ProofCreditsLogger.debug("GET Response Code :: " + responseCode2);
//            if (responseCode == HttpURLConnection.HTTP_OK)
//                ProofCreditsLogger.debug("Token Data Updated");
//            else
//                ProofCreditsLogger.debug("GET request not worked");
//
//            //update proofcredits.json
//
//            //write token, tokenchain, BNK00
//
//                writeToFile("tempToken",token,false);
//                String tokenHash = IPFSNetwork.add("tempToken",ipfs);
//                writeToFile(TOKENS_PATH+tokenHash,token,false);
//                deleteFile("tempToken");
//                writeToFile(TOKENCHAIN_PATH+tokenHash+".json","[]",false);
//                JSONObject temp = new JSONObject();
//                temp.put("tokenHash",tokenHash);
//                JSONArray tempArray = new JSONArray();
//                tempArray.put(temp);
//                updateJSON("add",PAYMENTS_PATH+"BNK00.json",tempArray.toString());
//
//            FileWriter File = new FileWriter(jsonFilePath);
//            File.write(records.toString());
//            File.close();
//
//            ProofCreditsLogger.debug("Updated balance of node : " + balance);
//                long endtime = System.currentTimeMillis();
//                Iterator<String> keys = InitiatorConsensus.quorumSignature.keys();
//                JSONArray signedQuorumList = new JSONArray();
//                while(keys.hasNext())
//                    signedQuorumList.put(keys.next());
//                APIResponse.put("did", receiverDidIpfsHash);
//                APIResponse.put("tid", tid);
//                APIResponse.put("token",token);
//                APIResponse.put("quorumlist",signedQuorumList);
//                APIResponse.put("time",endtime-starttime);
//                APIResponse.put("status", "Success");
//                APIResponse.put("message", level+tokenNumber);
//             }
//
//        }
//        else {
//            APIResponse.put("did", receiverDidIpfsHash);
//            APIResponse.put("tid", "null");
//            APIResponse.put("status", "Failed");
//            APIResponse.put("message", "Insufficent proofs");
//            ProofCreditsLogger.warn("Insufficient proof credits to mine");
//            return APIResponse;
//        }
//        return APIResponse;

//======================================================================================================================






        int level = 0,tokenNumber = 0,availableCredits = 0, balance=0;
        long starttime = System.currentTimeMillis();
        JSONArray resJsonData = new JSONArray();




        //Reading proofcredits.json
        String jsonFilePath = WALLET_DATA_PATH+"QuorumSignedTransactions.json";
        JSONArray records = new JSONArray(readFile(jsonFilePath));
        balance = records.length();
        JSONArray prooftid = new JSONArray();
        for (int i = 0; i < balance; i++) {
            JSONObject temp = records.getJSONObject(i);
            if(temp.getBoolean("minestatus")==false) {
                availableCredits++;
               //prooftid.put(temp.getString("tid"));
                // records.getJSONObject(i).put("minestatus",true);
            }
        }


        if (availableCredits>=creditUsed) {

            //String GET_URL = SYNC_IP+"/getInfo?count="+availableCredits;

            String GET_URL = SYNC_IP + "/minetoken";
            URL URLobj = new URL(GET_URL);
            HttpURLConnection con = (HttpURLConnection) URLobj.openConnection();
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                ProofCreditsLogger.debug("response from service " + response.toString());

                //JSONObject responseJSON=new JSONObject(response.toString());
                //resJsonData= responseJSON.getJSONArray("data");
                //creditUsed = responseJSON.getInt("credits");

                resJsonData = new JSONArray(response.toString());

            } else
                ProofCreditsLogger.debug("GET request not worked");


            //Check if node can mine token
            if (resJsonData.length() > 0) {
                //Calling Mine token function
                JSONArray token = new JSONArray();

                level = resJsonData.getJSONObject(0).getInt("level");

                for (int i = 0; i < resJsonData.length(); i++) {
                    token.put(Functions.mineToken(resJsonData.getJSONObject(i).getInt("level"), resJsonData.getJSONObject(i).getInt("token")));

                    creditUsed+=(int) Math.pow(2,(2+resJsonData.getJSONObject(i).getInt("level")));

                }

                if(resJsonData.getJSONObject(0).getInt("level")==1)
                    creditUsed = 10;

                String comments = resJsonData.toString() + prooftid;

                String authSenderByRecHash = calculateHash(token + receiverDidIpfsHash + comments, "SHA3-256");
                String tid = calculateHash(authSenderByRecHash, "SHA3-256");

                writeToFile("tempbeta", tid.concat(receiverDidIpfsHash), false);
                String betaHash = IPFSNetwork.add("tempbeta", ipfs);
                deleteFile("tempbeta");

                writeToFile("tempgamma", tid.concat(receiverDidIpfsHash), false);
                String gammaHash = IPFSNetwork.add("tempgamma", ipfs);
                deleteFile("tempgamma");


                JSONArray quorumArray= getQuorum(betaHash,gammaHash,receiverDidIpfsHash,receiverDidIpfsHash,token.length());


                for(int i=0;i<7;i++)
                {
                    alphaQuorum.put(quorumArray.getString(i));
                    betaQuorum.put(quorumArray.getString(7+i));
                    gammaQuorum.put(quorumArray.getString(14+i));
                }


                alphaPeersList=QuorumCheck(alphaQuorum,ipfs);
                betaPeersList= QuorumCheck(betaQuorum,ipfs);
                gammaPeersList=QuorumCheck(gammaQuorum,ipfs);

                // quorumPeersList = QuorumCheck(quorumArray, ipfs);

                if (alphaPeersList.size()<5||betaPeersList.size()<5||gammaPeersList.size()<5) {
                    updateQuorum(quorumArray,null,false,1);
                    APIResponse.put("did", receiverDidIpfsHash);
                    APIResponse.put("tid", "null");
                    APIResponse.put("status", "Failed");
                    APIResponse.put("message", "Quorum Members not available");
                    ProofCreditsLogger.warn("Quorum Members not available");
                    return APIResponse;
                }


                JSONObject dataObject = new JSONObject();
                dataObject.put("tid", tid);
                dataObject.put("message", comments);
                dataObject.put("receiverDidIpfs", receiverDidIpfsHash);
                dataObject.put("pvt", pvt);
                dataObject.put("senderDidIpfs", receiverDidIpfsHash);
                dataObject.put("token", token.toString());
                dataObject.put("alphaList", alphaPeersList);
                dataObject.put("betaList", betaPeersList);
                dataObject.put("gammaList", gammaPeersList);

                InitiatorProcedure.consensusSetUp(dataObject.toString(), ipfs, SEND_PORT + 3);

                if (!(InitiatorConsensus.quorumSignature.length() >= 3 * minQuorum(7))) {
                    APIResponse.put("did", receiverDidIpfsHash);
                    APIResponse.put("tid", "null");
                    APIResponse.put("status", "Failed");
                    APIResponse.put("message", "Consensus failed");
                    ProofCreditsLogger.debug("consensus failed");
                } else {
                    ProofCreditsLogger.debug("token mined " + token);

                    int counter = 0;
                    for (int i = 0; i < balance; i++) {
                        JSONObject temp = records.getJSONObject(i);
                        if (temp.getBoolean("minestatus") == false && (counter < creditUsed)) {
                            prooftid.put(temp.getString("tid"));
                            records.getJSONObject(i).put("minestatus", true);
                            counter++;
                        }
                    }


                    for (int i = 0; i < token.length(); i++) {
                        writeToFile("tempToken", token.getString(i), false);
                        String tokenHash = IPFSNetwork.add("tempToken", ipfs);
                        writeToFile(TOKENS_PATH + tokenHash, token.getString(i), false);
                        deleteFile("tempToken");
                        writeToFile(TOKENCHAIN_PATH + tokenHash + ".json", "[]", false);
                        JSONObject temp = new JSONObject();
                        temp.put("tokenHash", tokenHash);
                        JSONArray tempArray = new JSONArray();
                        tempArray.put(temp);
                        updateJSON("add", PAYMENTS_PATH + "BNK00.json", tempArray.toString());
                    }

                        writeToFile(jsonFilePath,records.toString(),false);
//                    FileWriter File = new FileWriter(jsonFilePath);
//                    File.write(records.toString());
//                    File.close();

                    ProofCreditsLogger.debug("Updated balance of node : " + (balance - creditUsed));
                    long endtime = System.currentTimeMillis();
                    totalTime=endtime-starttime;
                    Iterator<String> keys = InitiatorConsensus.quorumSignature.keys();
                    JSONArray signedQuorumList = new JSONArray();
                    while (keys.hasNext())
                        signedQuorumList.put(keys.next());

                    updateQuorum(quorumArray,signedQuorumList,true,1);
                    mineUpdate(receiverDidIpfsHash,creditUsed);
                    APIResponse.put("did", receiverDidIpfsHash);
                    APIResponse.put("tid", tid);
                    APIResponse.put("token", token);
                    APIResponse.put("creditsused", creditUsed);
                    APIResponse.put("quorumlist", signedQuorumList);
                    APIResponse.put("time", totalTime);
                    APIResponse.put("status", "Success");
                    APIResponse.put("message", token.length()+" tokens mined");

                    DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
                    Date date = new Date();
                    LocalDate currentTime = LocalDate.parse(formatter.format(date).replace("/", "-"));
                    JSONObject transactionRecord = new JSONObject();
                    transactionRecord.put("role", "Sender");
                    transactionRecord.put("tokens", token);
                    transactionRecord.put("txn", tid);
                    transactionRecord.put("quorumList",signedQuorumList);
                    transactionRecord.put("senderDID", receiverDidIpfsHash);
                    transactionRecord.put("receiverDID", receiverDidIpfsHash);
                    transactionRecord.put("Date", currentTime);
                    transactionRecord.put("totalTime", totalTime);
                    transactionRecord.put("comment", "minedtxn");
                    transactionRecord.put("essentialShare", InitiatorProcedure.essential);


                    JSONArray transactionHistoryEntry = new JSONArray();
                    transactionHistoryEntry.put(transactionRecord);
                    updateJSON("add", WALLET_DATA_PATH + "TransactionHistory.json", transactionHistoryEntry.toString());



                    if (!EXPLORER_IP.contains("127.0.0.1")) {


                        String url = EXPLORER_IP.concat("/CreateOrUpdateRubixToken");
                        URL obj = new URL(url);
                        HttpsURLConnection connection_Explorer = (HttpsURLConnection) obj.openConnection();

                        // Setting basic post request
                        connection_Explorer.setRequestMethod("POST");
                        connection_Explorer.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                        connection_Explorer.setRequestProperty("Accept", "application/json");
                        connection_Explorer.setRequestProperty("Content-Type", "application/json");
                        connection_Explorer.setRequestProperty("Authorization", "null");

                        // Serialization
                        JSONObject dataToSend = new JSONObject();
                        dataToSend.put("bank_id", "01");
                        dataToSend.put("user_did", receiverDidIpfsHash);
                        dataToSend.put("token_id", token);
                        dataToSend.put("level",level);
                        dataToSend.put("denomination", 1);
                        String populate = dataToSend.toString();

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("inputString", populate);
                        String postJsonData = jsonObject.toString();

                        // Send post request
                        connection_Explorer.setDoOutput(true);
                        DataOutputStream wr = new DataOutputStream(connection_Explorer.getOutputStream());
                        wr.writeBytes(postJsonData);
                        wr.flush();
                        wr.close();

                        int responseCodeExplorer = connection_Explorer.getResponseCode();
                        ProofCreditsLogger.debug("Sending 'POST' request to URL : " + url);
                        ProofCreditsLogger.debug("Post Data : " + postJsonData);
                        ProofCreditsLogger.debug("Response Code : " + responseCodeExplorer);

                        BufferedReader in_BR = new BufferedReader(
                                new InputStreamReader(connection_Explorer.getInputStream()));
                        String output;
                        StringBuffer response_Explorer = new StringBuffer();

                        while ((output = in_BR.readLine()) != null) {
                            response_Explorer.append(output);
                        }
                        in_BR.close();

                    }

                    if (!EXPLORER_IP.contains("127.0.0.1")) {
                        List<String> tokenList = new ArrayList<>();
                        for (int i = 0; i < token.length(); i++)
                            tokenList.add(token.getString(i));
                        String urlTxn = EXPLORER_IP+"/CreateOrUpdateRubixTransaction";
                        URL objTxn = new URL(urlTxn);
                        HttpsURLConnection conTxn = (HttpsURLConnection) objTxn.openConnection();

                        // Setting basic post request
                        conTxn.setRequestMethod("POST");
                        conTxn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                        conTxn.setRequestProperty("Accept", "application/json");
                        conTxn.setRequestProperty("Content-Type", "application/json");
                        conTxn.setRequestProperty("Authorization", "null");

                        // Serialization
                        JSONObject dataToSend = new JSONObject();
                        dataToSend.put("transaction_id", tid);
                        dataToSend.put("sender_did", receiverDidIpfsHash);
                        dataToSend.put("receiver_did", receiverDidIpfsHash);
                        dataToSend.put("token_id", tokenList);
                        dataToSend.put("token_time", (int) totalTime);
                        dataToSend.put("amount", 80);
                        String populate = dataToSend.toString();

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("inputString", populate);
                        String postJsonData = jsonObject.toString();

                        // Send post request
                        conTxn.setDoOutput(true);
                        DataOutputStream wrTxn = new DataOutputStream(conTxn.getOutputStream());
                        wrTxn.writeBytes(postJsonData);
                        wrTxn.flush();
                        wrTxn.close();

                        int responseCodeTxn = conTxn.getResponseCode();
                        ProofCreditsLogger.debug("Sending 'POST' request to URL : " + urlTxn);
                        ProofCreditsLogger.debug("Post Data : " + postJsonData);
                        ProofCreditsLogger.debug("Response Code : " + responseCode);

                        BufferedReader inTxn = new BufferedReader(
                                new InputStreamReader(conTxn.getInputStream()));
                        String outputTxn;
                        StringBuffer responseTxn = new StringBuffer();

                        while ((outputTxn = inTxn.readLine()) != null) {
                            responseTxn.append(outputTxn);
                        }
                        inTxn.close();

                        ProofCreditsLogger.debug(responseTxn.toString());
                    }

                }

            } else {
                APIResponse.put("did", receiverDidIpfsHash);
                APIResponse.put("tid", "null");
                APIResponse.put("status", "Failed");
                APIResponse.put("message", "error from mine service");
                ProofCreditsLogger.warn("error from mine service");
                return APIResponse;
            }
        }
        else
        {
            APIResponse.put("did", receiverDidIpfsHash);
            APIResponse.put("tid", "null");
            APIResponse.put("status", "Failed");
            APIResponse.put("message", "Insufficent proofs");
            ProofCreditsLogger.warn("Insufficient proof credits to mine");
            return APIResponse;
        }
        return APIResponse;
    }
}

