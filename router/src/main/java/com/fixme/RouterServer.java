package com.fixme;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.fixme.controlers.TimeMessage;

/**
 * RouterServer
 */
public class RouterServer {
	private int ports[] = new int[] { 5000, 5001 };
	private static long uID = 99999;
	// Router assigned id and Socket channel
	HashMap<String, HashMap<String, SocketChannel>> markets;
	// Router assigned id, broker id and Socket channel
	HashMap<String, HashMap<String, SocketChannel>> brokers;
	private SocketChannel marketChannel;
	private SocketChannel brokerChannel;

	public RouterServer() {
		this.markets = new HashMap<String, HashMap<String, SocketChannel>>();
		this.brokers = new HashMap<String, HashMap<String, SocketChannel>>();
	}

	public void newRouterServer() {
		try {
			Selector selector = Selector.open();

			for (int port : ports) {
				ServerSocketChannel ssChannel = ServerSocketChannel.open();
				ssChannel.configureBlocking(false);
				ServerSocket sSocket = ssChannel.socket();
				sSocket.bind(new InetSocketAddress(port));
				ssChannel.register(selector, SelectionKey.OP_ACCEPT);
			}

			System.out.println("\u001B[1;32mRouting server is now running...\u001B[0m");
			while (true) {
				if (selector.select() > 0) {
					performIO(selector);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void performIO(Selector s) {
		Iterator<SelectionKey> skIterator = s.selectedKeys().iterator();

		while (skIterator.hasNext()) {
			try {
				SelectionKey sKey = skIterator.next();
				if (sKey.isAcceptable()) {
					acceptConnection(sKey, s);
				} else if (sKey.isReadable()) {
					readWriteClient(sKey, s);
				} else {
					skIterator.remove();
				}
			} catch (IOException e) {
				skIterator.remove();
				return;
			}

			skIterator.remove();
		}
	}

	public void acceptConnection(SelectionKey sKey, Selector s) throws IOException {
		ServerSocketChannel ssChannel = (ServerSocketChannel) sKey.channel();
		SocketChannel sChannel = ssChannel.accept();
		sChannel.configureBlocking(false);
		sChannel.register(s, SelectionKey.OP_READ);

		switch (sChannel.socket().getLocalPort()) {
			case 5000: {
				TimeMessage.print("Broker connection!!!");
				break;
			}
			case 5001:
				TimeMessage.print("Market connection!!!");
				break;
		}
	}

	public void readWriteClient(SelectionKey sKey, Selector s) throws IOException {
		SocketChannel sChannel = (SocketChannel) sKey.channel();
		ByteBuffer cBuffer = ByteBuffer.allocate(1000);
		((Buffer) cBuffer).flip();
		((Buffer) cBuffer).clear();

		switch (sChannel.socket().getLocalPort()) {
			case 5000:
				processBrokerToMarket(cBuffer, sChannel);
				sChannel.register(s, SelectionKey.OP_READ);
				break;
			case 5001:
				processMarket(cBuffer, sChannel);
				sChannel.register(s, SelectionKey.OP_READ);
				break;
		}
	}

	public void processBrokerToMarket(ByteBuffer cBuffer, SocketChannel sc) throws IOException {
		String clientString;
		int count = sc.read(cBuffer);

		if (count > 0) {
			((Buffer) cBuffer).flip();
			clientString = Charset.forName("UTF-8").decode(cBuffer).toString().trim();
			// TimeMessage.print(clientString);
			if (Pattern.matches("new=\\d", clientString)) {
				Long id = this.nextID();
				HashMap<String, SocketChannel> blah = new HashMap<String, SocketChannel>();
				String respString = "connected=" + id;

				blah.put(Long.toString(id), sc);
				brokers.put(clientString.split("=")[1], blah);
				socketWrite(respString, sc, cBuffer);
			} else {
				String availableMarkets = "";
				if (clientString.equalsIgnoreCase("markets")) {
					availableMarkets = getAvailableMarkets().trim();
					if (availableMarkets.equals("")) {
						availableMarkets = "nada";
						socketWrite(availableMarkets, sc, cBuffer);
					} else
						socketWrite(availableMarkets, sc, cBuffer);
				} else {
					// String someString = this.broadcast(clientString, this.marketChannel);
					socketWrite("server message", sc, cBuffer);
				}
			}
		}
	}

	public void processMarket(ByteBuffer cBuffer, SocketChannel sc) throws IOException {
		String clientString;
		// if (this.marketChannel.isConnected()) {
		int count = sc.read(cBuffer);
		if (count > 0) {
			((Buffer) cBuffer).flip();
			clientString = Charset.forName("UTF-8").decode(cBuffer).toString();
			if (Pattern.matches("new=\\d", clientString)) {
				Long id = this.nextID();
				HashMap<String, SocketChannel> blah = new HashMap<String, SocketChannel>();
				String respString = "connected=" + id;

				blah.put(Long.toString(id), sc);
				markets.put(clientString.split("=")[1], blah);
				socketWrite(respString, sc, cBuffer);
			}
		}
	}

	public String broadcast(String msg, SocketChannel channel) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1000);
		((Buffer) bb).flip();
		((Buffer) bb).clear();
		bb.put(msg.getBytes());
		((Buffer) bb).flip();
		channel.write(bb);

		Selector selector = Selector.open();
		channel.register(selector, SelectionKey.OP_READ);
		while (true) {
			if (selector.select() > 0) {
				Iterator<SelectionKey> i = selector.selectedKeys().iterator();
				while (i.hasNext()) {
					try {
						SelectionKey sk = i.next();
						if (sk.isReadable()) {
							SocketChannel mChannel = (SocketChannel) sk.channel();
							((Buffer) bb).flip();
							((Buffer) bb).clear();

							int count = mChannel.read(bb);
							if (count > 0) {
								bb.rewind();
								String response = Charset.forName("UTF-8").decode(bb).toString();
								i.remove();
								return response;
							}
						}
						i.remove();
					} catch (IOException e) {
						System.out.println("++++++++++++++++++++++++++++++++++++");
						// e.printStackTrace();
					}
				}
			}
		}
	}

	public long nextID() {
		return (++uID);
	}

	public SocketChannel getMarketChannel() {
		return this.marketChannel;
	}

	public SocketChannel getBrokerChannel() {
		return this.brokerChannel;
	}

	public String getAvailableMarkets() {
		List<String> ids = new ArrayList<String>();
		List<String> clids = new ArrayList<String>();
		for (String key : this.markets.keySet()) {
			HashMap<String, SocketChannel> gg = markets.get(key);
			try {
				ByteBuffer bb = ByteBuffer.allocate(15);
				((Buffer) bb).flip();

				socketWrite("connection test", gg.get(gg.keySet().stream().findFirst().get()), bb);
				ids.add(key);
				clids.add(gg.keySet().stream().findFirst().get());

			} catch (IOException e) {
				markets.remove(key);
				return getAvailableMarkets();
			}
		}
		String retIds = String.join(",", ids);
		String retClids = String.join(",", clids);
		return retIds + "|" + retClids;
	}

	public void socketWrite(String msg, SocketChannel sc, ByteBuffer bb) throws IOException {
		((Buffer) bb).flip();
		((Buffer) bb).clear();
		bb.put(msg.getBytes());
		((Buffer) bb).flip();
		bb.rewind();
		sc.write(bb);
	}
}
