package com.fixme;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * RouterServer
 */
public class RouterServer {
	private int ports[] = new int[] { 5000, 5001 };
	private SocketChannel marketChannel;
	private SocketChannel brokerChannel;

	public RouterServer() {
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

			System.out.println("Routing server is now running...");
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
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			skIterator.remove();
		}
	}

	public void acceptConnection(SelectionKey sKey, Selector s) throws IOException {
		ServerSocketChannel ssChannel = (ServerSocketChannel) sKey.channel();
		SocketChannel sChannel = ssChannel.accept();

		switch (sChannel.socket().getLocalPort()) {
		case 5000:
			System.out.println("Connection from Broker is got!!!");
			sChannel.configureBlocking(false);
			sChannel.register(s, SelectionKey.OP_READ);
			break;
		case 5001:
			System.out.println("Connection from Market is got!!!");
			sChannel.configureBlocking(false);
			sChannel.register(s, SelectionKey.OP_READ);
			break;
		}
	}

	public void readWriteClient(SelectionKey sKey, Selector s) throws IOException {
		SocketChannel sChannel = (SocketChannel) sKey.channel();
		ByteBuffer cBuffer = ByteBuffer.allocate(1000);
		cBuffer.flip();
		cBuffer.clear();

		switch (sChannel.socket().getLocalPort()) {
		case 5000:
			this.brokerChannel = sChannel;
			processBrokerToMarket(cBuffer);
			this.marketChannel.register(s, SelectionKey.OP_WRITE);
			break;
		case 5001:
			this.marketChannel = sChannel;
			processMarketToBroker(cBuffer);
			this.brokerChannel.register(s, SelectionKey.OP_WRITE);
			break;
		}

	}

	// Problem might be here

	public void processBrokerToMarket(ByteBuffer cBuffer) throws IOException {
		String clientString;
		if (this.marketChannel.isConnected()) {
			int count = this.brokerChannel.read(cBuffer);
			if (count > 0) {
				cBuffer.flip();
				clientString = Charset.forName("UTF-8").decode(cBuffer).toString();
				System.out.println("B to M ++++> " + clientString);
				this.broadcast(clientString, this.marketChannel);
			}
		}
	}

	// Problem might be here 2

	public void processMarketToBroker(ByteBuffer cBuffer) throws IOException {
		String clientString;
		if (this.brokerChannel.isConnected()) {
			int count = this.marketChannel.read(cBuffer);
			if (count > 0) {
				cBuffer.flip();
				clientString = Charset.forName("UTF-8").decode(cBuffer).toString();
				System.out.println("M to B ++++> " + clientString);
				// this.broadcast(clientString, this.marketChannel);

				// cBuffer.flip();
				// cBuffer.clear();
				// // cBuffer.put(processClientRequest(input).getBytes());
				// cBuffer.flip();
				// cBuffer.rewind();
				// this.marketChannel.write(cBuffer);
				// this.marketChannel.close();
			}
		}
	}

	public void broadcast(String msg, SocketChannel channel) throws IOException {
		ByteBuffer bb = ByteBuffer.allocate(1000);
		bb.flip();
		bb.clear();
		bb.put(msg.getBytes());
		bb.flip();
		channel.write(bb);
	}

	public SocketChannel getMarketChannel() {
		return this.marketChannel;
	}

	public SocketChannel getBrokerChannel() {
		return this.brokerChannel;
	}
}
