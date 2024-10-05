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


public class breaker1{

	private static String ext = ".enc";
	private static byte[] pass;
	private static int passSize = 1024;


	public static void main(String[] args) throws Exception{
		String fileName = args[0];
		File fileF = new File(fileName);
		if(!fileF.exists()){
			System.out.println("File " + fileName + " does not exist");
			return;
		}
		RandomAccessFile file = new RandomAccessFile(fileF, "rw");

		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
		    extension = fileName.substring(i+1);
		}

		if(!extension.equals("enc")){
			System.out.println("Wrong file type");
			return;
		}

		long end = file.length() - 4;
		file.seek(end);
		int f_token = file.readInt();
		file.seek(0);

		int password = Integer.MIN_VALUE;
		int token = Integer.hashCode(password + Integer.hashCode(password + Integer.hashCode(password)));

		while(token != f_token){
        	password++;
			token = Integer.hashCode(password + Integer.hashCode(password + Integer.hashCode(password)));
		}

		file.setLength(end);
		byte[] line = new byte[1024];
		int size;

		while((size = file.read(line)) != -1){
			file.seek(file.getChannel().position() - size);
			file.write(crypt(line, password), 0, size);
		}

		file.close();
		renameFile(fileName, extension, i);
		System.out.println("File decrypted");

	}


    private static byte[] crypt(byte[] bytes, int password){
        Random random = new Random(password);
        
        byte[] values = new byte[bytes.length];
        random.nextBytes(values);
        
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


    public static String insert(String bag, String marble, int index) {
    	String bagBegin = bag.substring(0,index);
    	String bagEnd = bag.substring(index);
    	return bagBegin + marble + bagEnd;
	}
}