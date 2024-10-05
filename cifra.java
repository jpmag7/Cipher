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

public class cifra{

	private static int bufferSize = 1024;
	private static int passSize = 1024;
	private static String ext = ".enc";
	private static byte[] pass;

	public static void main(String[] args) throws Exception{
		if(args.length < 1){
			System.out.println("Invalid arguments\ncifra <file> <password>");
			return;
		}
		File file = new File(args[0]);
		if(!file.exists()){
			System.out.println("File " + args[0] + " does not exist");
			return;
		}

		byte[] password = rand(new String(System.console().readPassword("Password:")).getBytes(), passSize);

		if(file.isDirectory() && args.length == 3 && args[1].equals("-r")){
			
			List<String> l = new ArrayList<>();
        	listOfFiles(file, l, args[0], true);
        	for(String f : l) cryptFile(f, password);
        	
        	System.out.println("Done");
        	return;
    	}
    	else if(file.isDirectory() && args.length == 3 && args[1].equals("-a")){
    		List<String> l = new ArrayList<>();
        	listOfFiles(file, l, args[0], false);
        	for(String f : l) cryptFile(f, password);
        	
        	System.out.println("Done");
        	return;
    	}
    	else if(file.isDirectory()){
    		System.out.println("Given File is a folder");
        	return;
    	}
    	else cryptFile(args[0], password);

        System.out.println("Done");
	}


	private static void cryptFile(String fileName, byte[] password) throws Exception{
		pass = password;
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

		byte[] token = rand(rand(password, passSize), passSize);

		if(extension.equals(ext.substring(1))){
			long end = file.length() - passSize;
			file.seek(end);
			byte[] f_token = new byte[passSize];
			file.read(f_token);
			if(!Arrays.equals(token, f_token)){
				System.out.println("Incorrect password for file " + fileName);
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
            System.out.println("Error on SHA512");
        }
        return null;
    }
}