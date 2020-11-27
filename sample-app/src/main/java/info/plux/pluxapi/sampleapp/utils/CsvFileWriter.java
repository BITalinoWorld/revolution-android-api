/*
 *
 * Copyright (c) PLUX S.A., All Rights Reserved.
 * (www.plux.info)
 *
 * This software is the proprietary information of PLUX S.A.
 * Use is subject to license terms.
 *
 */

package info.plux.pluxapi.sampleapp.utils;

import android.content.Context;
import android.os.Environment;
import info.plux.pluxapi.bitalino.BITalinoFrame;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvFileWriter {
    // Definition of relevant class variables
    private FileWriter csvWriter;
    private String separator = "\t";
    private String lineSeparator = "\n";

    // Definition/Initialisation of class constants
    private final String filePrefix = "BITalino_record_";
    private final String fileExtension = ".txt";
    private final int nbrFixedCols = 5; // nSeq | I1 | I2 | O1 | O2

    /**
     * Class constructor used to initialise the base CSVWriter object with the location of the
     * CSV file that will be created and filled.
     *
     * @param context Context where the current instance was created.
     * @param folderName Name of the folder where the file will be stored.
     * @param isExternalStorage Flag identifying if the previous folder should be located (true)
     *                          or not (false) in the external storage of the Android device.
     * @param separator Symbol used to separate columns of our CSV file.
     */
    public CsvFileWriter(Context context, String folderName, boolean isExternalStorage,
                         String separator) {
        this(context, folderName, isExternalStorage);

        // Define the new separator.
        this.separator = separator;
    }

    /**
     * Class constructor used to initialise the base CSVWriter object with the location of the
     * CSV file that will be created and filled.
     *
     * @param context Context where the current instance was created.
     * @param folderName Name of the folder where the file will be stored.
     * @param isExternalStorage Flag identifying if the previous folder should be located (true)
     *                          or not (false) in the external storage of the Android device.
     */
    public CsvFileWriter(Context context, String folderName, boolean isExternalStorage) {
        // Define the path as a function of the isExternalStorage flag.
        final String folderPath = isExternalStorage ?
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + folderName :
                context.getFilesDir() + File.separator + folderName;

        // Create the directory if it does not exist yet.
        final File file = new File(folderPath);
        final boolean dirCreated = file.mkdir();

        // Generate a unique filename.
        final long currTimestamp = System.currentTimeMillis() / 1000;
        final String filename = filePrefix + currTimestamp;

        // Initialisation of OpenCSV writer object.
        try {
            csvWriter = new FileWriter(folderPath + File.separator + filename + fileExtension);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method responsible to fill the file opened in the constructor with BITalino frames
     * (samples collected during the real-time acquisition).
     *
     * @param frame BITalinoFrame containing data streamed by BITalino (from the multiple active
     *              channels).
     */
    public void writeFrameToFile(BITalinoFrame frame){
        // Create an array that will contain data to be printed in the current line of the file.
        final int[] rawData = frame.getAnalogArray();
        final String[] lineArray = new String[nbrFixedCols + rawData.length];

        // Fill the temporary array.
        try {
            // [Sequence number - which reboots every 15 samples]
            csvWriter.write(frame.getSequence() + separator);
            // [Digital Inputs - I1 and I2]
            csvWriter.write(frame.getDigital(0) + separator); // I1
            csvWriter.write(frame.getDigital(1) + separator); // I2
            csvWriter.write(frame.getDigital(2) + separator); // O1
            csvWriter.write(frame.getDigital(3) + separator); // O2
            for(int i = 5; i < lineArray.length; i++){
                if(i != lineArray.length - 1) {
                    csvWriter.write(rawData[i - nbrFixedCols] + separator);
                } else{
                    csvWriter.write(rawData[i - nbrFixedCols] + lineSeparator);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to close the file once the data streaming ends.
     */
    public void closeCsvFile() {
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
