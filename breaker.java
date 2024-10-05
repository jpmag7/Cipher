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


public class breaker{

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

		long end = file.length() - passSize;
		file.seek(end);
		byte[] f_token = new byte[passSize];
		file.read(f_token);
		file.seek(0);

		byte[] password = new byte[passSize];
		byte[] token = rand(rand(password, passSize), passSize);

		Random random = new Random();

		while(!Arrays.equals(token, f_token)){
        	random.nextBytes(password);
			token = rand(rand(password, passSize), passSize);
		}

		file.setLength(end);
		byte[] line = new byte[1024];
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

		file.close();
		renameFile(fileName, extension, i);
		System.out.println("File decrypted");

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
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(seed);
                if (data != null)
                    md.update(data);
                data = md.digest();
                ret.write(data, 0, Math.min(n - ret.size(), data.length));
            }
            return ret.toByteArray();
        } catch (Exception e) {
            System.out.println("Error on SHA256");
        }
        return null;
    }
}