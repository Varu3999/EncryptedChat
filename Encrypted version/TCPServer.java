import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

class TCPServer 
{
	Hashtable<String, Pair<String, Socket[]>> map;
	ServerSocket welcomeSocket;
	TCPServer(int port_number)
	{
		try
		{
			welcomeSocket = new ServerSocket(port_number);
			map = new Hashtable<>();
		}
		catch(Exception e)
		{
			System.out.println(e);
		}		
	}
	
    public static void main(String argv[]) throws Exception
    {        
        TCPServer wa_server = new TCPServer(1234);
        while(true)
        {
            Socket conn_socket = wa_server.welcomeSocket.accept();
            (new ServerThread(conn_socket, wa_server)).start();
        }
    }
}


class ServerThread extends Thread
{
    Socket socket;
    String my_name;
    Hashtable<String, Pair<String, Socket[]>> user_info;

    ServerThread(Socket socket, TCPServer wa_server)
    {
        this.socket = socket;
        user_info = wa_server.map;
    }

    public void run()
    {
        
        try
        {
            while(true)
            {
    
                String serverSentence = "error";
                String clientSentence;
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream  outToClient = new DataOutputStream(socket.getOutputStream());
                clientSentence = inFromClient.readLine();
                // System.out.println(clientSentence);

                String[] split_clientSentence = clientSentence.split(" ");

                if(split_clientSentence[0].equals("REGISTER"))
                {
                    
                    if(split_clientSentence.length>3)
                    {
                        serverSentence = "ERROR 100 Malformed username\n";
                        outToClient.writeBytes(serverSentence);
                    }
                    if(split_clientSentence[1].equals("TOSEND"))
                    {
                        
                        String username = split_clientSentence[2];
                        my_name = username;
                        if(isCorrectUsername(username))
                        {
                            serverSentence = "REGISTERED TOSEND " + username +"\n\n";
                            // Username and sockets stored in a Hashmap
                            // {username, rec_socket}
                            Socket[] sockets = new Socket[2];
                            sockets[0] = socket;
                            String key_len = inFromClient.readLine();
                            int key_length =  Integer.parseInt(key_len);
                            char[]temp1=new char[key_length];
                            inFromClient.read(temp1, 0, key_length);
                            String public_key = String.valueOf(temp1);
                            //System.out.println(public_key);                            
                            Pair<String, Socket[]> p = new Pair<String, Socket[]>(public_key, sockets);
                            user_info.put(username, p);    
                            outToClient.writeBytes(serverSentence);                      
                        }
                        else
                        {
                            serverSentence = "ERROR 100 Malformed username\n\n";
                            outToClient.writeBytes(serverSentence);
                        }
                    }
                    else if(split_clientSentence[1].equals("TORECV"))
                    {
                        
                        inFromClient.readLine();
                        String username = split_clientSentence[2];
                        if(isCorrectUsername(username))
                        {
                            serverSentence = "REGISTERED TORECV " + username +"\n\n";
                            System.out.println(user_info.get(username));
                            Socket[] sockets1 = user_info.get(username).getValue();                            
                            sockets1[1] = socket;  
                            //System.out.print(socket);
                            outToClient.writeBytes(serverSentence);
                            this.stop();
                                            
                        }
                        else
                        {
                            serverSentence = "ERROR 100 Malformed username\n";
                            outToClient.writeBytes(serverSentence);
                        }
                            
                        
                    }                 
                                     
                }
                else if(split_clientSentence[0].equals("SEND"))
                {
                    // Reads the username from the message
                    String user_to_send = split_clientSentence[1];
                    System.out.println("Sending message to " + user_to_send);

                    // Reads the content length from the message
                    clientSentence = inFromClient.readLine();
                    int content_length;
                    split_clientSentence = clientSentence.split(": ");
                    content_length = Integer.parseInt(split_clientSentence[1]);

                    // Reads the message from the client
                    inFromClient.readLine();
                    char[]temp=new char[content_length];
                    inFromClient.read(temp, 0, content_length);
                    String sending_message = String.valueOf(temp);
                    try{
                        // Finds the username from the map formed
                        Socket[] sockets11 = user_info.get(user_to_send).getValue();
                        Socket rec_socket_rec = sockets11[1];
                        String rec_sentence;                        
                        DataOutputStream outToRecp = new DataOutputStream(rec_socket_rec.getOutputStream());
                        outToRecp.writeBytes("FORWARD " + my_name + "\n" + "Content-length: " + content_length + "\n\n" + sending_message);
                        BufferedReader inFromRecp = new BufferedReader(new InputStreamReader(sockets11[1].getInputStream()));                          
                        rec_sentence = inFromRecp.readLine();
                        inFromRecp.readLine();
                        if(rec_sentence.equals("RECEIVED " + my_name))
                        {
                            outToClient.writeBytes("SENT " + user_to_send + "\n\n");
                        }                                  
                        
                    }catch(Exception e){
                        outToClient.writeBytes("ERROR 101 UNABLE TO SEND\n\n");
                    }
                }
                else
                {
                    System.out.println("Unable to send");
                }
            }
            
        }
        catch(Exception e)
        {
            System.out.println(e);
            user_info.remove(my_name);
            System.out.println("DEREGISTERED " + my_name);
        }

    }

    public Boolean isCorrectUsername(String username)
    {
        Boolean f = username.matches("[a-zA-Z0-9]+");
        if(f)
        {
            if(user_info.contains(username))
            {
                f = false;
            }
        }
        return f;
    }
}