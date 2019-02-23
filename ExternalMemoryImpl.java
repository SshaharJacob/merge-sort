import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ExternalMemoryImpl implements IExternalMemory {

	private int blockSize = 7000000;
	private int sizeOfChar = 2;
	private boolean flagPartC = false;
	private String selectStr = "";
	private int colToSelect = 0;

	/**
	 * sort a given temporary file, according to a given column
	 * @param tempFile - the file to sort
	 * @param colIndex - the start index of the column to sort
	 * @param outName - the output name of this temp file
	 * @param numOfLines - the number of lines in the file
	 * @param tmpPath - the path to store this file
	 */
	private void sortTempFile(File tempFile, int colIndex, String outName, int numOfLines, String tmpPath){
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile));
			String line;
			String[] sortColumn = new String[numOfLines];
			for (int i = 0; i < numOfLines; i++){
				line = bufferedReader.readLine();
				sortColumn[i] = line;
			}

			Arrays.sort(sortColumn, new Comparator<String>(){
				@Override
				public int compare(String str1, String str2) {
					return str1.substring(colIndex,colIndex + 20).compareTo(str2.substring(colIndex,
							colIndex + 20));
				}
			});

			BufferedWriter myWriter = new BufferedWriter(new FileWriter(tmpPath + outName));
			for(int i = 0; i < numOfLines; i++){
				myWriter.write(sortColumn[i] + '\n');
			}
			myWriter.close();
			bufferedReader.close();
		}
		catch (IOException e){e.printStackTrace();}
	}

	/**
	 * updates an array with a given string - puts the new string at the end of the array and throws away
	 * the first one in the array. the first element in the array would be the minimal string and last
	 * would be the maximal string (or null).
	 * @param array - the array to update
	 * @param newStr - the new string to put at the end of the array
	 */
	private void updateArray(String[] array, String newStr){
		int len = array.length;
		for (int i = 0; i < len - 1; i++){
			array[i] = array[i+1];
		}
		array[len-1] = newStr;
	}

	/**
	 * merges sorted files into one sorted file
	 * @param numberOfFiles - the number of files to merge
	 * @param tmpPath - the path to store this file
	 * @param maxSize - the maximal size of byets to read into the main memory
	 * @param colIndex - the start index of the column to sort
	 * @param totalLines - the total number of lines to be in the final output file
	 * @param out - path & name of the final output file
	 */
	private void mergeFiles(int numberOfFiles, String tmpPath, int maxSize, int colIndex, int totalLines, String
			out){
		int linesToRead = maxSize / (numberOfFiles + 1), counter = 0;
		String[] lines = new String[linesToRead];
		String[][] unSorted = new String[numberOfFiles][linesToRead];

		try{
			BufferedReader[] bufferArray = new BufferedReader[numberOfFiles];
			File outputFile = new File(out);
			if (!outputFile.exists())
			{
				outputFile.getParentFile().mkdirs();
				outputFile.createNewFile();

			}
			BufferedWriter myWriter = new BufferedWriter(new FileWriter(outputFile));


			for (int i = 1; i <= numberOfFiles; i++){
				File file = new File(tmpPath + Integer.toString(i));
				BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
				bufferArray[i-1] = bufferedReader;
				String line = bufferedReader.readLine();
				while (line != null){
					lines[counter] = line;
					counter++;
					if (counter >= linesToRead){break;}
					line = bufferedReader.readLine();
				}
				unSorted[i-1] = lines;
				lines = new String[linesToRead];
				counter = 0;

			}
			int loopCounter = 0;
			String minStr = unSorted[0][0];
			int minIndex = 0;
			while (loopCounter < totalLines){

				for (int i = 0; i < numberOfFiles; i++) {
					if (unSorted[i][0] != null){
						if (minStr.substring(colIndex, colIndex + 20).compareTo(unSorted[i][0].substring
								(colIndex, colIndex + 20)) > 0) {
							minStr = unSorted[i][0];
							minIndex = i;
						}
					}
				}

				myWriter.write(minStr + '\n');
				String newStr = bufferArray[minIndex].readLine();
				updateArray(unSorted[minIndex], newStr);

				for (int i = 0; i < numberOfFiles; i++){
					if (unSorted[i][0] != null){
						minStr = unSorted[i][0];
						minIndex = i;
						break;
					}
					if (i == numberOfFiles -1){
						loopCounter = totalLines;
					}
				}

				loopCounter++;
			}
			for(int i = 0; i < numberOfFiles; i++){
				bufferArray[i].close();
			}
			myWriter.close();
		}

		catch (IOException e){e.printStackTrace();}
	}

	@Override
	public void sort(String in, String out, int colNum, String tmpPath)  {
		int colIndex = colNum == 1 ? 0 : (colNum - 1) * 20 + 1;
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(in)));
			String line = bufferedReader.readLine();
			int lineSize = sizeOfChar * line.length();
			int maxSize = blockSize / lineSize;
			int counter = 0, fileNumber = 1, writetime = 0, sortTempFileTime = 0;
			String[] lines = new String[maxSize];
			Path tempFile = Paths.get(tmpPath + "temp.txt");
			int totalLines = 0;
			while (line != null){
				while (counter != maxSize && line != null){
					boolean select = false;
					if (flagPartC)
					{
						int toSelect = colToSelect == 1 ? 0 : (colToSelect - 1) * 20 + 1;
						select = line.substring(toSelect, toSelect + 20).contains(selectStr);
					}

					if (!flagPartC || select)
					{
						lines[counter] = line;
						counter++;
						totalLines++;
					}
					line = bufferedReader.readLine();
				}
				Files.write(tempFile, Arrays.asList(lines), Charset.forName("UTF-8"));
				sortTempFile(tempFile.toFile(), colIndex, Integer.toString(fileNumber), counter, tmpPath);

				fileNumber++;
				tempFile = Paths.get(tmpPath + "temp.txt");
				counter = 0;
			}
			mergeFiles(fileNumber-1, tmpPath, maxSize, colIndex, totalLines, out);

			// deletes all temp files.
			Files.deleteIfExists(Paths.get(tmpPath + "temp.txt"));
			for (int i = 1; i < fileNumber; i++)
			{
				Files.deleteIfExists(Paths.get(tmpPath + i));
			}

			bufferedReader.close();
		} catch (IOException e) {e.printStackTrace();}
	}

	@Override
	/*
	  @param in the pathname of the file to read from (pathname includes the file name)
	 * @param out​ – the pathname of the file to write to (pathname includes the file name).
	 * @param colNumSelect – the column number for the selection operation.
	 * @param tmpPath​ – the path for saving temporary files.
	 * @param substrSelect – a string to check whether it’s a substring of the given column in a line.
	 */
	public void select(String in, String out, int colNumSelect, String substrSelect, String tmpPath) {
		int colIndex = colNumSelect == 1 ? 0 : (colNumSelect - 1) * 20 + 1;

		try{
			File outputFile = new File(out);
			if (!outputFile.exists())
			{
				outputFile.getParentFile().mkdirs();
				outputFile.createNewFile();

			}
			BufferedWriter myWriter = new BufferedWriter(new FileWriter(outputFile));
			BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(in)));
			String line;
			while ((line = bufferedReader.readLine()) != null){
				if (line.substring(colIndex, colIndex + 20).contains(substrSelect)){
					myWriter.write(line + '\n');
				}
			}
			myWriter.close();
			bufferedReader.close();
		}catch (IOException e) {e.printStackTrace();}

	}

	@Override
	public void sortAndSelectEfficiently(String in, String out, int colNumSort,
										 String tmpPath, int colNumSelect, String substrSelect) {
		selectStr = substrSelect;
		flagPartC = true;
		colToSelect = colNumSelect;
		sort(in, out, colNumSort, tmpPath);
		flagPartC = false;
	}


}