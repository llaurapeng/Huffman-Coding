import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Ow	en Astrachan
 *
 * Revise
 */

public class HuffProcessor {

	private class HuffNode implements Comparable<HuffNode> {
		HuffNode left;
		HuffNode right;
		int value;
		int weight;

		public HuffNode(int val, int count) {
			value = val;
			weight = count;
		}
		public HuffNode(int val, int count, HuffNode ltree, HuffNode rtree) {
			value = val;
			weight = count;
			left = ltree;
			right = rtree;
		}

		public int compareTo(HuffNode o) {
			return weight - o.weight;
		}
	}

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private boolean myDebugging = false;
	
	public HuffProcessor() {
		this(false);
	}
	
	public HuffProcessor(boolean debug) {
		myDebugging = debug;
	}

	public void compress(BitInputStream in, BitOutputStream out) {
		int[] vals = getCounts(in);
		HuffNode root = makeTree(vals);
		in.reset();
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTree(root, out);
		String[] encodings = new String[ALPH_SIZE + 1];
		makeEncodings(root, "", encodings);

		while (true) {
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1)
				break;

			String encode = encodings[val];
			if (encode != null)
				out.writeBits(encode.length(), Integer.parseInt(encode, 2));
		}

		String pseudo = encodings[PSEUDO_EOF];

		out.writeBits(pseudo.length(), Integer.parseInt(pseudo, 2));

		out.close();
	}

	private void writeTree(HuffNode root, BitOutputStream out) {
		if (root.value == -1) {
			throw new HuffException("bad input");
		}
		if (root.right == null && root.left == null) {
			out.writeBits(1, 1);
			out.writeBits(1 + BITS_PER_WORD, root.value);
		} else {
			out.writeBits(1, 0);
			writeTree(root.left, out);
			writeTree(root.right, out);
		}
	}

	private void makeEncodings(HuffProcessor.HuffNode root, String string, String[] encodings) {
		if (root.right == null && root.left == null) {
			encodings[root.value] = string;
			return;
		} else {
			makeEncodings(root.left, string + "0", encodings);
			makeEncodings(root.right, string + "1", encodings);
		}

	}

	private HuffProcessor.HuffNode makeTree(int[] counts) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for (int k = 0; k < counts.length; k++) {
			if (counts[k] > 0)
				pq.add(new HuffNode(k, counts[k], null, null));
		}
		pq.add(new HuffNode(PSEUDO_EOF, 1, null, null));
		// account for PSEUDO_EOF having a single occurrence

		while (pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			// create new HuffNode t with weight from
			// left.weight+right.weight and left, right subtrees
			HuffNode t = new HuffNode(0, left.weight + right.weight, left, right);
			pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}

	private int[] getCounts(BitInputStream in) {
		int[] vals = new int[ALPH_SIZE + 1];
		while (true) {
			int i = in.readBits(BITS_PER_WORD);
			if (i == -1) {
				break;
			}
			vals[i] = vals[i] + 1;
		}
		vals[PSEUDO_EOF] = 1;
		return vals;
	}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out) {


		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("invalid magic number" + bits);
		}
		HuffNode root = readTree(in);
		HuffNode curr = root;

		while (true) {
			bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			} else {
				if (bits == 0)
					curr = curr.left;
				else
					curr = curr.right;

				if (curr.right == null&&curr.left == null) {
					if (curr.value == PSEUDO_EOF)
						break;
					else {
						out.writeBits(BITS_PER_WORD, curr.value);
						curr = root;
					}
				}
			}

		}
		out.close();
	}

	private HuffNode readTree(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1)
			throw new HuffException("Invalid EOF");
		if (bit == 0) {
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);
			return new HuffNode(0, 0, left, right);
		} else {
			int value = in.readBits(BITS_PER_WORD + 1);
			return new HuffNode(value, 0, null, null);
		}
	}

}