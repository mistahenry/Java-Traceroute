import org.savarese.vserv.tcpip.*; 
import com.savarese.rocksaw.net.RawSocket; 
import java.net.*; 
import java.io.*; 

public class UDPTraceroute {

	public static void main(String args[]){

		int destPort = getDestPort(args);
		if(destPort < 0){
			System.out.println("Must enter a valid destination port");
			System.exit(0);
		} //if
		int maxHops = getMaxHops(args);
		if(maxHops < 0){
			System.out.println("Must enter a max hops > 0");
			System.exit(0);
		} //if
		String ipString = getIPAddress(args);
		if(ipString.equals("")){
			System.out.println("Must enter a destination IP Address");
			System.exit(0);
		} //if
		InetAddress ip = null;
		String localHostIp = "";
		try{
			ip = InetAddress.getByName(ipString);
			localHostIp = InetAddress.getByName("myip.local").getHostAddress();
		} //try
		catch(Exception e){
			e.printStackTrace();
		} //catch

		System.out.println("UDP traceroute from "+localHostIp+" to "+ip.getHostAddress()+" with max hops of "+maxHops+"\n");
		
		//UDP payload and other variable declarations
		byte[] payload = "CSCI4760 Jason Beck and Will Henry".getBytes();
		int ttl = 1;
		long time = 0;
		boolean found = false;

		while(!found && maxHops >= ttl){
			try{

				UDPPacket udp = new UDPPacket(payload.length + 28); //plus 28 for UDP & IP header
				udp.setIPVersion(4);
				udp.setIPHeaderLength(5);
				udp.setProtocol(IPPacket.PROTOCOL_UDP);
				udp.setTTL(ttl);
		
				byte[] host = new byte[4]; //destination host
				byte[] localhost = new byte[4];
				String[] tokens = ipString.split("\\.");
				String[] tokens1 = localHostIp.split("\\.");
				for(int i = 0; i < tokens.length; i++){
					host[i] = (byte)Integer.parseInt(tokens[i]);
					localhost[i] = (byte)Integer.parseInt(tokens1[i]);
				} //for
			
				udp.setDestinationAsWord(OctetConverter.octetsToInt(host));
				udp.setSourceAsWord(OctetConverter.octetsToInt(localhost));
				udp.setDestinationPort(destPort);
				udp.setSourcePort(12345);
				udp.setUDPPacketLength(payload.length + 8); //plus 8 for UDP header
				udp.setUDPDataByteLength(payload.length);
		
				byte[] buffer = new byte[udp.size()];
				udp.getData(buffer);
				System.arraycopy(payload, 0, buffer, 28, payload.length);
				udp.setData(buffer);
				udp.computeIPChecksum(true);
				udp.computeUDPChecksum(true);
				udp.getData(buffer);

				RawSocket rs = new RawSocket(); //send socket
				System.out.println("here");
				rs.open(RawSocket.PF_INET, RawSocket.getProtocolByName("udp"));
				RawSocket rs2 = new RawSocket(); //receive socket
				rs2.open(RawSocket.PF_INET, RawSocket.getProtocolByName("icmp"));
				rs.setIPHeaderInclude(true);

				time = System.nanoTime();
				rs.write(ip,buffer);
				rs.close();
				buffer = new byte[rs2.getReceiveBufferSize()];
					
				rs2.setReceiveTimeout(2500);
				int length = rs2.read(buffer); //waiting to receive packet. If it times out, goes to catch statement
				time = System.nanoTime() - time;
				UDPPacket pack = new UDPPacket(length);
				pack.setData(buffer);
				ICMPEchoPacket icmp = new ICMPEchoPacket(length);
				icmp.setData(buffer);

				System.out.println("TTL: " +ttl+ "\t| HopIP: "+pack.getSourceAsInetAddress().getHostAddress()+"\t| Time: "+(float)time/1000000+"ms\n");
				if(icmp.getCode() == 3 || pack.getSourceAsInetAddress().getHostAddress().equals(ipString)){
					found = true;
					System.out.println("Destination found!");
				}
				rs2.close();

			} catch (Exception e) {
				System.out.println("TTL: " +ttl+ "\t| HopIP: X.X.X.X"+"\t| Time: Timeout\n");
			} //catch

			ttl++;

		} //while loop

		if(maxHops == ttl - 1){
			System.out.println("Destination NOT found");
		} //if

	} //main

	public static int getDestPort(String[] args){
		for(int i = 0; i < args.length; i++){
			if(args[i].equals("-p")){
				return Integer.parseInt(args[i+1]);
			}
		}
		return -1;
	}//getDestPort

	public static int getMaxHops(String[] args){
		for(int i = 0; i <args.length; i++){
			if(args[i].equals("-h")){
				return Integer.parseInt(args[i+1]);
			}
		}
		return -1;
	}//getMaxHops

	public static String getIPAddress(String[] args){
		if(args.length < 5){
			return "";
		}
		String ipString = args[args.length-1];
		return ipString;
	}//getIPAddress

}
