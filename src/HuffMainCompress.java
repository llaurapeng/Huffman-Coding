import java.io.*;

public class HuffMainCompress {
	private final static String HUFF_EXTENSION = ".hf";

	private static String getDefaultFileName(String name) {
		System.out.println("name: "+name);
		if (name.endsWith(".uhf")) {
			return name.substring(0,name.length()-4)+HUFF_EXTENSION;
		}
		return name + HUFF_EXTENSION;
	}

	public static void main(String[] args) {
		
		System.out.println("Huffman Compress Main");
		System.out.println("Use FileDialog to choose file to compress");
		File inf = FileSelector.selectFile("file to compress");
		if (inf == null) {
			System.err.println("input file cancelled");
			return;
		}
		System.out.println("Use FileDialog to choose file name/directory for compressed file");
		String saveName = getDefaultFileName(inf.getName());
		File outf = FileSelector.saveFile("compressed file name",saveName);
		if (outf == null) {
			System.err.println("input or output file cancelled");
			return;
		}
		BitInputStream bis = new BitInputStream(inf);
		BitOutputStream bos = new BitOutputStream(outf);
		HuffProcessor hp = new HuffProcessor();
		long before = System.nanoTime();
		hp.compress(bis, bos);
		long after = System.nanoTime();
		System.out.printf("compress from %s to %s\n", 
		                   inf.getName(),outf.getName());
		System.out.printf("file: %d bits to %d bits\n",inf.length()*8,outf.length()*8);
		System.out.printf("read %d bits, wrote %d bits\n", 
				           bis.bitsRead(),bos.bitsWritten());
		long diff = bis.bitsRead() - bos.bitsWritten();
		System.out.printf("bits saved = %d\n",diff);
		System.out.printf("Compress took %d milliseconds", (after-before)/1000000);
	}
}