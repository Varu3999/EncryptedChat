import java.io.*;
import java.net.*;
class Client {

    public static int port = 1234;
    public String userName = "";
    public static String hostIP = "localhost";
    public Socket clientSocketSen;
    public Socket clientSocketRec;

    public static void main(String argv[]) 
    {
        try{
            Client ob = new Client();
            ob.registerToSend();
            ob.registerToReceive();
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            Receiver rec = new Receiver(ob.clientSocketRec);
            rec.start();
            System.out.println("You can send message by typing @(username) (message) and press enter");
            String message = "";
            String to = "";
            while(true){    
                String msg = inFromUser.readLine();
                if(msg.charAt(0) == '@'){
                    String[] msgSplit = msg.split(" ",2);
                    if(msgSplit[1].length() > 0 && msgSplit[0].length() > 0){
                        message = msgSplit[1];
                        msgSplit = msgSplit[0].split("@",2);
                        to = msgSplit[1];
                        ob.sendMessage(to , message);
                    }else{
                        System.out.println("WRONG FORMAT!!");
                    }

                }else{
                    System.out.println("WRONG FORMAT!!");
                }
            }
        }catch(Exception e){
            System.out.println("Server is DOWN!!!!!!!");
        }
        
    }

    private void sendMessage(String to , String message) throws Exception
    {
        DataOutputStream outToServer = new DataOutputStream(clientSocketSen.getOutputStream());
        outToServer.writeBytes("SEND " + to + "\nContent-length: " + message.length() + "\n\n" + message);
        // outToServer.writeBytes("SEND " + to);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketSen.getInputStream()));
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");
        if(!splitRes[0].equals("SENT")){
            if(splitRes[0].equals("ERROR") && splitRes[1] == "102"){
                System.out.println("Unable To Send ....");
            }else{
                System.out.println("Header Incomplete ....");
            }
        }else{
            System.out.println("Message Sent Successfully ....!!");
        }
    }

    private void registerToSend() throws Exception
    {

        System.out.println("User Name:");
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        clientSocketSen = new Socket(hostIP, port);
        
        DataOutputStream outToServer = new DataOutputStream(clientSocketSen.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketSen.getInputStream()));
        userName = inFromUser.readLine();
        outToServer.writeBytes("REGISTER TOSEND " + userName + "\n\n");
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");

        if(!(splitRes[0].equals("REGISTERED") && splitRes[1].equals("TOSEND") && splitRes[2].equals(userName))){
            System.out.println("NOT A VALID USER NAME OR USERNAME ALREADY REGISTERED!!!");
            clientSocketSen.close();
            registerToSend();
        }

    }

    private void registerToReceive() throws Exception
    {
        clientSocketRec = new Socket(hostIP, port);
        DataOutputStream outToServer = new DataOutputStream(clientSocketRec.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketRec.getInputStream()));
        outToServer.writeBytes("REGISTER TORECV " + userName + "\n\n");
        String response = inFromServer.readLine();
        String[] splitRes = response.split(" ");
        if(!(splitRes[0].equals("REGISTERED") && splitRes[1].equals("TORECV") && splitRes[2].equals(userName))){
            clientSocketRec.close();
            registerToReceive();
        }
    }
}

class Receiver extends Thread{
    public Socket socketRec;
    public BufferedReader inFromServer;
    DataOutputStream outToServer;
    public Receiver(Socket socket) throws Exception{
        socketRec = socket;
        inFromServer = new BufferedReader(new InputStreamReader(socketRec.getInputStream()));
        outToServer = new DataOutputStream(socketRec.getOutputStream());
    }
    @Override
    public void run(){
        try{
            while(true){
                try{
                    String finalMsg = "";
                    String sender = "";
                    String response = inFromServer.readLine();
                    
                    String[] splitRes = response.split(" ");
                    finalMsg += splitRes[1];
                    sender = splitRes[1];
                    response = inFromServer.readLine();
                    splitRes = response.split(": ");
                    int contentLength = Integer.parseInt(splitRes[1]);
                    response = inFromServer.readLine();
                    char[] message = new char[contentLength];;
                    inFromServer.read(message , 0 , contentLength);
                    response = String.valueOf(message);
                    finalMsg += ": " + response;
                    outToServer.writeBytes("RECEIVED " + sender + "\n\n");
                    System.out.println(finalMsg);
                }catch(Exception e){
                    //outToServer.writeBytes("ERROR 103 Header incomplete\n\n");
                    System.out.println(e);
                }   
            }
        }
        catch(Exception e){
            System.out.println(e);
        }

    }
}