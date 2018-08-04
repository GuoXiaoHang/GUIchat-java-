package pkg;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GUIchat extends Frame {
	
	private TextArea viewArea;
	private TextArea sendArea;
	private TextField tf;
	private Button btn_send;
	private Button btn_record;
	private Button btn_clear;
	private Button btn_shake;
	private DatagramSocket sendSocket;
	private BufferedWriter bw;
	private final String logFileName = "record.txt";

	public GUIchat() {
		this.setLocation(400, 40);
		this.setSize(600, 800);
		addSouthPanel();
		addCenterPanel();
		addListener();
		try {
			sendSocket = new DatagramSocket();
			bw = new BufferedWriter(new FileWriter(logFileName, true));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		new RcvThread().start();
		this.setVisible(true);
	}
	
	private void addListener() {
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				sendSocket.close();
				try {
					bw.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.exit(0);
			}
		});
		
		sendArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					try {
						send();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		
		btn_record.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				showRecord();
			}
		});
		
		btn_clear.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				viewArea.setText("");
			}
		});
		
		btn_shake.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				send("__shake__", tf.getText());
			}
		});
		
		btn_send.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				try {
					send();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	protected void shake() {
		int x = this.getLocation().x;
		int y = this.getLocation().y;
		final int timeout = 50;
		for (int i = 0; i < 3; i++) {
			try {
				this.setLocation(x + i*5, y);
				Thread.sleep(timeout);
				this.setLocation(x + i*5, y + i*5);
				Thread.sleep(timeout);
				this.setLocation(x, y + i*5);
				Thread.sleep(timeout);
				this.setLocation(x, y);
				Thread.sleep(timeout);
				this.setLocation(x - i*5, y);
				Thread.sleep(timeout);
				this.setLocation(x - i*5, y - i*5);
				Thread.sleep(timeout);
				this.setLocation(x, y - i*5);
				Thread.sleep(timeout);
				this.setLocation(x, y);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void showRecord() {
		FileInputStream fis = null;
		ByteArrayOutputStream baos = null;
		
		int len;
		byte[] arr = new byte[8192];
		try {
			bw.flush();
			fis = new FileInputStream(logFileName);
			baos = new ByteArrayOutputStream();
			while ((len = fis.read(arr)) != -1) {
				baos.write(arr, 0, len);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String log = baos.toString();
		viewArea.setText(log);
		try {
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void addCenterPanel() {
		Panel centerPanel = new Panel();
		viewArea = new TextArea();
		viewArea.setEditable(false);
		sendArea = new TextArea(4, 1);
		sendArea.setFont(new Font("xxx", Font.PLAIN, 18));
		viewArea.setFont(new Font("xxx", Font.PLAIN, 20));
		centerPanel.setLayout(new BorderLayout());
		centerPanel.add(viewArea, BorderLayout.CENTER);
		centerPanel.add(sendArea, BorderLayout.SOUTH);
		this.add(centerPanel, BorderLayout.CENTER);
	}

	private void addSouthPanel() {
		Panel southPanel = new Panel();
		tf = new TextField(14);
		btn_send = new Button("Send");
		btn_record = new Button("Record");
		btn_clear = new Button("Clear");
		btn_shake = new Button("Shake");
		southPanel.add(tf);
		southPanel.add(btn_send);
		southPanel.add(btn_record);
		southPanel.add(btn_clear);
		southPanel.add(btn_shake);
		this.add(southPanel, BorderLayout.SOUTH);
	}
	
	private void send(String info, String ip) { 
		try {
			DatagramPacket packet = new DatagramPacket(info.getBytes(), info.getBytes().length, InetAddress.getByName(ip), 9999);
			sendSocket.send(packet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void send() throws Exception {
		String info = sendArea.getText();
		String ip = tf.getText();
		send(info, ip);
		String time = getCurrentTime();
		String str = time + " " + InetAddress.getLocalHost().getHostAddress() + "\n" + info + "\n";
		bw.write(str);
		viewArea.append(str);
		sendArea.setText("");
	}

	private String getCurrentTime() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		return sdf.format(date);
	}
	
	private class RcvThread extends Thread {
		public void run() {
			try {
				DatagramSocket RcvSocket = new DatagramSocket(9999);
				DatagramPacket packet = new DatagramPacket(new byte[1024 * 8], 1024 * 8);
				while (true) {
					RcvSocket.receive(packet);
					byte[] buf = packet.getData();
					int len = packet.getLength();
					String info = new String(buf, 0, len);
					if ("__shake__".equals(info)) {
						shake();
						continue;
					}
					String time = getCurrentTime();
					String str = time + " " + packet.getAddress().getHostAddress() + "\n" + info + "\n";
					bw.write(str);
					viewArea.append(str);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	public static void main(String[] args) {
		new  GUIchat();
	}

}
