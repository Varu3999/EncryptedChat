# Chat Application (Encrypted with Signature) [![java:v8](https://img.shields.io/badge/Java-v8-brightgreen.svg)](https://www.java.com)

This is a Chat Engine built on Java using the Socket Library. It contains a Server which acts as an interface between Client and helps in exchange of messages. This project is a part of Computer Networks course COL 334 taught by Prof. Aaditeshwar Seth.

# Features

  - Multiple Clients can be connected to the Server and Chat at the same time.
  - Once connected to the Server the Client doesn't need to reconnect to send or receive new messages
  - The Server can run in 3 mode Non-Encrypted Mode / Encrypted Mode / Encrypted Mode with Signature Check
  - You can set the mode of the server while setting it up and all the clients will connect to it in the same mode automatically
 
### Modes of Operations
- **Non Encrypted** : This is the basic mode of operation in which the chating betwen the clients is not encrypted and anyone who has the access to the server can read the messages between the 2 clients. This isn't a secure way of communication as the message can be seen if packet sniffing tools are used.
- **Encrypted** : This is a more secure mode of communication as in this mode the chating between the clients is encrypted so even if someone tries to sniff the packets or has the control over the server, he/she won't be able to see the messages. This encryption is done by generation a private key and public key pair of all clients and giving the public key to the server. Whenever a Client A wants to send a message to Client B the he will encrypt the message using public key of B and then only Client B can read the message as it has the private key to decrypt it. But there is one problem with this approach. If the Client A send the messge to Client B but by changing the header of the packet such that the Sender becomes Client C then Client A won't be able to detect this.
- **Encrypted with Signature Check** : This is the most secure way of communication as in the mode while sending the message the Client A also has send a Signature by hashing the encrypted message and encrypt it again using his own private key and send both Signature and the encrypted messages so that when Client B receives the message it will first check whether the message is really from Client A by checking if the Signature matches the message.
 
 
# How to Use

**Downoload the project in the directory you want**

```sh
$ git clone https://github.com/Varu3999/EncryptedChat
$ cd EncryptedChat
$ cd Combined
```

**Start the Server**

```sh
$ javac TCPServer.java
$ java TCPServer
```

When the server starts, select the mode from the 3 specified modes and press Enter.
The server would start running on your localhost:1234

**Start a Client**

```sh
$ javac Client.java
$ java Client
```

Enter the ip of the server if you are running on the same machine simply put 'localhost' 
Now Select your Username and Start Chatting with your Friends 😄!!!

# Contributors
- Varun Gupta
- Rushang Gupta
