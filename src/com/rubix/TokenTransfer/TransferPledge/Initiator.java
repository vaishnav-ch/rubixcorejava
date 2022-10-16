package com.rubix.TokenTransfer.TransferPledge;

import com.rubix.Resources.IPFSNetwork;
import io.ipfs.api.IPFS;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;

import static com.rubix.Resources.Functions.*;
import static com.rubix.Resources.IPFSNetwork.forward;
import static com.rubix.Resources.IPFSNetwork.swarmConnectP2P;

public class Initiator {
    public static Logger PledgeInitiatorLogger = Logger.getLogger(Initiator.class);
    public static JSONArray pledgedNodes  = new JSONArray();
    public static JSONObject abortReason = new JSONObject();

    public static boolean abort = false;

    public static boolean pledgeSetUp(String data, IPFS ipfs, int PORT) throws JSONException, IOException {
        PledgeInitiatorLogger.debug("Initiator Calling");
        abortReason = new JSONObject();
        abort = false;
        pledgedNodes = new JSONArray();
        JSONArray nodesToPledgeTokens = new JSONArray();
        JSONObject dataObject = new JSONObject(data);
        JSONArray alphaList = dataObject.getJSONArray("alphaList");

        Socket qSocket = null;
        int tokensCount = dataObject.getInt("amount");
        for (int i = 0; i < alphaList.length(); i++) {

            PropertyConfigurator.configure(LOGGER_PATH + "log4jWallet.properties");

            String quorumID = alphaList.getString(i);
            swarmConnectP2P(quorumID, ipfs);
            syncDataTable(null, quorumID);
            String quorumDidIpfsHash = getValues(DATA_PATH + "DataTable.json", "didHash", "peerid", quorumID);
            String quorumWidIpfsHash = getValues(DATA_PATH + "DataTable.json", "walletHash", "peerid", quorumID);
            nodeData(quorumDidIpfsHash, quorumWidIpfsHash, ipfs);
            String appName = quorumID.concat("alphaPledge");

            PledgeInitiatorLogger.debug("Quorum ID " + quorumID + " AppName " + appName);
            forward(appName, PORT, quorumID);
            PledgeInitiatorLogger.debug("Connected to " + quorumID + "on port " + (PORT) + "with AppName" + appName);

            qSocket = new Socket("127.0.0.1", PORT);
            BufferedReader qIn = new BufferedReader(new InputStreamReader(qSocket.getInputStream()));
            PrintStream qOut = new PrintStream(qSocket.getOutputStream());

            JSONObject dataArray = new JSONObject();
            dataArray.put("amount", tokensCount);
            dataArray.put("tokenList", dataObject.getJSONArray("tokenList"));
            dataArray.put("senderPID", getPeerID(DATA_PATH.concat("DID.json")));
            qOut.println(dataArray.toString());
            PledgeInitiatorLogger.debug("Sent request to alpha node: " + quorumID);


            String qResponse = "";
            try {
                qResponse = qIn.readLine();
            } catch (SocketException e) {
                PledgeInitiatorLogger.warn("Quorum " + quorumID + " is unable to respond!");
                IPFSNetwork.executeIPFSCommands("ipfs p2p close -t /p2p/" + quorumID);
                qSocket.close();
            }


            if (qResponse != null) {
                JSONObject pledgeObject = new JSONObject(qResponse);
                if (pledgeObject.getString("pledge").equals("Tokens")) {
                    PledgeInitiatorLogger.debug("Quorum " + quorumID + " is pledging Tokens");
                    JSONArray tokenDetails = pledgeObject.getJSONArray("tokenDetails");
                    for (int k = 0; k < tokenDetails.length(); k++) {

                        JSONObject tokenObject = tokenDetails.getJSONObject(k);
                        JSONArray tokenChain = tokenObject.getJSONArray("chain");
                        JSONObject genesisObject = tokenChain.getJSONObject(0);
                        if (genesisObject.has("pledgeToken")) {
                            PledgeInitiatorLogger.debug("This token has already been pledged - Aborting");
                            PledgeInitiatorLogger.debug("4. Setting abort to true");
                            abortReason.put("Quorum", quorumID);
                            abortReason.put("Reason", "Token " + tokenObject.getString("tokenHash") + " has already been pledged");
                            abort = true;
                            qSocket.close();
                            return abort;
                        }
                    }
                    PledgeInitiatorLogger.debug("tokensCount: " + tokensCount);
                    JSONObject quorumDetails = new JSONObject();
                    quorumDetails.put("ID", quorumID);
                    quorumDetails.put("count", tokenDetails.length());
                    nodesToPledgeTokens.put(quorumDetails);
                    PledgeInitiatorLogger.debug("tokensSentToPledge: " + tokenDetails.length());
                    tokensCount -= tokenDetails.length();
                    PledgeInitiatorLogger.debug("Contacted Node "+ quorumID + "  for Pledging  - Over with abort status: " + abort);
                    IPFSNetwork.executeIPFSCommands("ipfs p2p close -t /p2p/" + quorumID);
                    qSocket.close();
                    PledgeInitiatorLogger.debug("tokensCount: " + tokensCount);
                    if(tokensCount == 0)
                        break;

                }else{
                    IPFSNetwork.executeIPFSCommands("ipfs p2p close -t /p2p/" + quorumID);
                    qSocket.close();
                }
            } else {
                PledgeInitiatorLogger.debug("6. Setting abort to true");
                abortReason.put("Quorum", quorumID);
                abortReason.put("Reason", "Response is null");
                abort = true;
                PledgeInitiatorLogger.debug("Response is null");
                IPFSNetwork.executeIPFSCommands("ipfs p2p close -t /p2p/" + quorumID);
                qSocket.close();
                return abort;
            }
        }

        PledgeInitiatorLogger.debug("Closing all connections...");
        for (int i = 0; i < alphaList.length(); i++) {
            String quorumID = alphaList.getString(i);
            IPFSNetwork.executeIPFSCommands("ipfs p2p close -t /p2p/" + quorumID);
            qSocket.close();
        }

        PledgeInitiatorLogger.debug("Token Count: " + tokensCount);
        if(tokensCount > 0) {
            PledgeInitiatorLogger.debug("Quorum Nodes cannot Pledge, Aborting...");
            abortReason.put("Reason", "Quorum Nodes cannot Pledge, Aborting...");
            abort = true;
            return true;
        }

        int tokensPledged = dataObject.getInt("amount");
        if (!abort) {
            PledgeInitiatorLogger.debug("Tokens can be pledged from " + nodesToPledgeTokens);
            for (int j = 0; j < nodesToPledgeTokens.length(); j++) {
                String quorumID = nodesToPledgeTokens.getJSONObject(j).getString("ID");
                String appName = quorumID.concat("alphaPledge");

                PledgeInitiatorLogger.debug("Quorum " + quorumID + " pledging");
                PledgeInitiatorLogger.debug("Quorum ID " + quorumID + " AppName " + appName);
                forward(appName, PORT, quorumID);
                PledgeInitiatorLogger.debug("Connected to " + quorumID + "on port " + (PORT) + "with AppName" + appName);

                Socket qSocket1 = new Socket("127.0.0.1", PORT);
                BufferedReader qIn = new BufferedReader(new InputStreamReader(qSocket1.getInputStream()));
                PrintStream qOut = new PrintStream(qSocket1.getOutputStream());


                JSONObject object = new JSONObject();
                object.put("PledgeTokens", true);
                object.put("amount", nodesToPledgeTokens.getJSONObject(j).getInt("count"));
                object.put("senderPID", getPeerID(DATA_PATH.concat("DID.json")));
                qOut.println(object.toString());
                PledgeInitiatorLogger.debug("Sent request to alpha node: " + quorumID + ": " + object.toString());

                String qResponse = "";
                try {
                    qResponse = qIn.readLine();
                } catch (SocketException e) {
                    PledgeInitiatorLogger.warn("Quorum " + quorumID + " is unable to respond!");
                    IPFSNetwork.executeIPFSCommands("ipfs p2p close -t /p2p/" + quorumID);
                }

                if (qResponse != null) {
                    JSONArray tokenDetails = new JSONArray(qResponse);
                    JSONArray newChains = new JSONArray();
                    for (int k = 0; k < tokenDetails.length(); k++) {
                        JSONObject tokenObject = tokenDetails.getJSONObject(k);
                        JSONArray tokenChain = tokenObject.getJSONArray("chain");
                        JSONObject genesisObject = tokenChain.getJSONObject(0);
                        if (genesisObject.has("pledgeToken")) {
                            PledgeInitiatorLogger.debug("7. Setting abort to true because token has been already pledged");
                            abortReason.put("Quorum", quorumID);
                            abortReason.put("Reason", "Token " + tokenObject.getJSONArray("tokenHash") + " has been already pledged");
                            abort = true;
                            qSocket1.close();
                            return abort;
                        } else {
                            genesisObject.put("pledgeToken", dataObject.getJSONArray("tokenList").getString(k));

                            tokenChain.remove(0);
                            JSONArray newTokenChain = new JSONArray();
                            newTokenChain.put(genesisObject);
                            for (int l = 0; l < tokenChain.length(); l++) {
                                newTokenChain.put(tokenChain.getJSONObject(l));
                            }
                            newChains.put(newTokenChain);
                            tokensPledged -= nodesToPledgeTokens.getJSONObject(j).getInt("count");
                        }
                    }
                    pledgedNodes = nodesToPledgeTokens;
                    if (abort) {
                        qOut.println("Abort");
                        PledgeInitiatorLogger.debug("Quorum " + quorumID + " Aborted as already Pledged");
                        return abort;
                    } else {
                        qOut.println(newChains.toString());
                        PledgeInitiatorLogger.debug("Quorum " + quorumID + " Pledged");
                    }
                } else {
                    PledgeInitiatorLogger.debug("8. Setting abort to true");
                    abortReason.put("Quorum", quorumID);
                    abortReason.put("Reason", "Response is null");
                    abort = true;
                    qSocket1.close();
                    PledgeInitiatorLogger.debug("Alpha Quorum " + quorumID + " Replied Null (abort status true)");
                    return abort;
                }

                IPFSNetwork.executeIPFSCommands("ipfs p2p close -t /p2p/" + quorumID);
                qSocket1.close();

            }
        }
        if(tokensPledged > 0)
            abort = true;

        PledgeInitiatorLogger.debug("Quorum Verification with Tokens or Credits with Abort Status:" + Initiator.abort);
        return abort;
    }

}