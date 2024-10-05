import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.datatransfer.*;
import java.awt.Dimension;
import java.util.*;
import java.nio.*;
import java.io.*;
import java.awt.dnd.*;
import java.util.Arrays;
import java.io.RandomAccessFile;
import java.io.File;
import java.util.Random;
import java.util.ArrayList;
import java.nio.file.Paths;
import java.util.List;
import java.security.MessageDigest;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.awt.event.*;
import javax.swing.*;

public class DragDropCifra {


    public static void main(String[] args) {

        JFrame f=new JFrame("Password Field Example");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setResizable(false);
	    final JLabel label = new JLabel();            
	    label.setBounds(20,150, 200,50);  
	    final JPasswordField value = new JPasswordField();   
	    value.setBounds(100,75,100,30);  
        JLabel l2=new JLabel("Password:");    
        l2.setBounds(20,75, 80,30);    
        JButton b = new JButton("OK");  
        b.setBounds(100,120, 80,30);   
        f.add(value); f.add(l2); f.add(b);  
        f.setSize(300,300);    
        f.setLayout(null);    
        f.setVisible(true);     
        b.addActionListener(new ActionListener() {  
        public void actionPerformed(ActionEvent e) {
           DragDropTestFrame(f, value.getText()); 
        }  
     });   

    }

    public static void DragDropTestFrame(JFrame f, String pass) {

        // Set the frame title
        //super("Drag and drop test");
        f.getContentPane().removeAll();

        // Create the label
        JLabel myLabel = new JLabel("Drag something here!", SwingConstants.CENTER);
        myLabel.setBounds(0,-30, 300,300);

        // Create the drag and drop listener
        MyDragDropListener myDragDropListener = new MyDragDropListener(pass, myLabel);

        // Connect the label with a drag and drop listener
        new DropTarget(myLabel, myDragDropListener);

        // Add the label to the content
        f.getContentPane().add(BorderLayout.CENTER, myLabel);

        f.revalidate();
		f.repaint();
		f.setVisible(true);

    }

}


class MyDragDropListener implements DropTargetListener {

	private static int bufferSize = 1024;
	private static int passSize = 1024;
	private static String ext = ".enc";
	private static byte[] pass;
	private static byte[] password;
	private static JLabel myLabel;
	private static boolean errorOcorred = false;

	public MyDragDropListener(String passS, JLabel myLabel){
		password = rand(passS.getBytes(), passSize);
		this.myLabel = myLabel;
	}

    @Override
    public void drop(DropTargetDropEvent event) {

        // Accept copy drops
        event.acceptDrop(DnDConstants.ACTION_COPY);

        // Get the transfer which can provide the dropped item data
        Transferable transferable = event.getTransferable();

        // Get the data formats of the dropped item
        DataFlavor[] flavors = transferable.getTransferDataFlavors();

        // Loop through the flavors
        for (DataFlavor flavor : flavors) {

            try {

                // If the drop items are files
                if (flavor.isFlavorJavaFileListType()) {

                    // Get all of the dropped files
                    List<File> files = (List<File>) transferable.getTransferData(flavor);

                    // Loop them through
                    for (File file : files) {

                        if(file.isDirectory()){
				    		List<String> l = new ArrayList<>();
				        	listOfFiles(file, l, file.getPath().toString(), false);
				        	for(String f : l) cryptFile(f, password);
				        }
				    	else cryptFile(file.getPath().toString(), password);

				    }
				    if(!errorOcorred) System.exit(0);

                }

            } catch (Exception e) {

                // Print out the error stack
                e.printStackTrace();

            }
        }

        // Inform that the drop is complete
        event.dropComplete(true);

    }

    @Override
    public void dragEnter(DropTargetDragEvent event) {
    }

    @Override
    public void dragExit(DropTargetEvent event) {
    }

    @Override
    public void dragOver(DropTargetDragEvent event) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event) {
    }


    private static void cryptFile(String fileName, byte[] password) throws Exception{
		pass = password;
		File fileF = new File(fileName);

		if(!fileF.exists()){
			myLabel.setText("File " + fileName.substring(fileName.lastIndexOf(File.separator) + 1) + " does not exist");
			errorOcorred = true;
			return;
		}
		RandomAccessFile file = new RandomAccessFile(fileF, "rw");

		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(i+1);
		}

		byte[] token = rand(rand(password, passSize), passSize);

		if(extension.equals(ext.substring(1))){
			long end = file.length() - passSize;
			file.seek(end);
			byte[] f_token = new byte[passSize];
			file.read(f_token);
			if(!Arrays.equals(token, f_token)){
				myLabel.setText("Incorrect password for file " + fileName.substring(fileName.lastIndexOf(File.separator) + 1));
				errorOcorred = true;
				file.close();
				return;
			}
			file.setLength(end);
			file.seek(0);
		}

		byte[] line = new byte[bufferSize];
		int size;

		while((size = file.read(line)) != -1){
			file.seek(file.getChannel().position() - size);
			byte[] toCrypt = new byte[size + passSize];
			System.arraycopy(line, 0, toCrypt, 0, size);
			System.arraycopy(pass, 0, toCrypt, size, passSize);
			byte[] crypted = crypt(toCrypt, pass);
			pass = Arrays.copyOfRange(crypted, size, size + passSize);
			file.write(crypted, 0, size);
		}

		if(!extension.equals(ext.substring(1)))
			file.write(token);

		file.close();
		renameFile(fileName, extension, i);
	}


    private static byte[] crypt(byte[] bytes, byte[] password){
        byte[] values = rand(password, bytes.length);
        
        int i = 0;
        for (byte b : bytes)
            bytes[i] = (byte) (b ^ values[i++]);
        
        return bytes;
    }


    private static void renameFile(String fileName, String extension, int i){
		File fileOld = new File(fileName);

		File fileNew = extension.equals(ext.substring(1)) ? new File(fileName.substring(0, i)) : new File(fileName + ext);
    	if (fileNew.exists())
   			fileNew = new File(insert(fileName, "(1)", fileName.indexOf('.')) + ext);

   		fileOld.renameTo(fileNew);
    }


    public static void listOfFiles(File dirPath, List<String> l, String op, boolean rec){
        File filesList[] = dirPath.listFiles();
        for(File file : filesList) {
           if(file.isFile()) {
              l.add(op == "" ? file.getName() : Paths.get(op, file.getName()).toString());
           } else if(rec){
              listOfFiles(file, l, op == "" ? file.getName() : Paths.get(op, file.getName()).toString(), rec);
           }
        }
    }


    public static String insert(String bag, String marble, int index) {
    	String bagBegin = bag.substring(0,index);
    	String bagEnd = bag.substring(index);
    	return bagBegin + marble + bagEnd;
	}



	public static byte[] rand(byte[] seed, int n) {
        try {
            byte[] data = null;
            ByteArrayOutputStream ret = new ByteArrayOutputStream(n);
            while (ret.size() < n) {
                MessageDigest md = MessageDigest.getInstance("SHA512");
                md.update(seed);
                if (data != null)
                    md.update(data);
                data = md.digest();
                ret.write(data, 0, Math.min(n - ret.size(), data.length));
            }
            return ret.toByteArray();
        } catch (Exception e) {
            myLabel.setText("Error on SHA512");
        }
        return null;
    }

}