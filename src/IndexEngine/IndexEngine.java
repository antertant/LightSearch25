package IndexEngine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class IndexEngine {
    static Integer INTERNAL_ID = 0;
    static LinkedHashMap<String, Integer> LEXICON = new LinkedHashMap<>();
    static LinkedHashMap<Integer, ArrayList<Integer>> POSTING = new LinkedHashMap<>();
    static LinkedHashMap<String, String> MAPPER = new LinkedHashMap<>();
    static LinkedHashMap<Integer, Integer> WORDCOUNT = new LinkedHashMap<>();

    public static void main(String[] args){

        if(args.length != 2){
            System.out.println("\nError: Invalid Command.\n");
            System.out.println("////////////////////////////////////////////////////////////////////////");
            System.out.println("\nYou should follow commands below to execute it:\n");
            System.out.println("java IndexEngine.IndexEngine [Path of Documents' gzip File] [Path of Storage]\n");
            System.out.println("////////////////////////////////////////////////////////////////////////");
            System.exit(1);
        }

        String originalDocPath = args[0];
        String storagePath = args[1];

//        String originalDocPath = "D:\\UWaterloo\\MSCI_720\\A4\\latimes.gz";
//        String storagePath = "D:\\UWaterloo\\MSCI_720\\A4\\latimesDatabaseStemmed";
//        stemmer = "1";

        File testFile = new File(storagePath);
        if(testFile.exists()) {
            System.out.print("\nError: Storage directory already exists.\n");
            System.exit(1);
        }
        try {
            BufferedReader docStream = readDocs(originalDocPath);
            processDocs(docStream, storagePath);
        }
        catch (IOException e) {
            System.out.println("Error: Reading files failed.");
        }
    }

    private static BufferedReader readDocs(String docPath) throws IOException{

        InputStream fileStream = new FileInputStream(docPath);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream);

        return new BufferedReader(decoder);
    }

    private static void processDocs(BufferedReader docStream, String storePath) throws IOException{
        String docLine = "Initial";
        List<String> singleDoc = new ArrayList<>();
        while(docLine != null){
            while(!docLine.equals("</DOC>")){
                docLine = docStream.readLine();
                singleDoc.add(docLine);
            }

            outputDoc(singleDoc, storePath);

            singleDoc.clear();
            docLine = docStream.readLine();
            singleDoc.add(docLine);
        }
        outputPosting(storePath);
        outputLexicon(storePath);
        outputMapper(storePath);
    }

    private static void outputDoc(List<String> singleDoc, String storePath) throws IOException {
        String docNo = "";
        String docText = "";
        String currentLine;
        String docHead = "THIS DOCUMENT HAS NO HEADLINE!";
        List<String> metaData = new ArrayList<>();
        List<String> docTokens;
        int docTokenSize;

        int singleDocSize = singleDoc.size();
        int currentIndex = 0;

        for(; currentIndex < singleDocSize; currentIndex++) {
            currentLine = singleDoc.get(currentIndex);
            if (currentLine.contains("<DOCNO>")) {
                docNo = currentLine.substring(7, currentLine.indexOf("</DOCNO>")).trim();
            }
            else if(currentLine.equals("<HEADLINE>")){
                currentIndex += 2;
                docHead = singleDoc.get(currentIndex);
                docText = docText.concat(docHead).concat("\n");
                while(!singleDoc.get(++currentIndex).equals("</P>")) {
                    docHead = docHead.concat(singleDoc.get(currentIndex));
                    docText = docText.concat(docHead).concat("\n");
                }
                while(!(currentLine = singleDoc.get(++currentIndex)).equals("</HEADLINE>")) {
                    if (!(currentLine.equals("<P>") || currentLine.equals("</P>"))){
                        docText = docText.concat(currentLine).concat("\n");
                    }
                }
//                docHead = docHead.trim();
            }
            else if(currentLine.equals("<TEXT>")){
                currentIndex++;
                while (!(currentLine = singleDoc.get(currentIndex)).equals("</TEXT>")){
                    if (!(currentLine.equals("<P>") || currentLine.equals("</P>"))){
                        docText = docText.concat(currentLine).concat("\n");
                    }
                    currentIndex++;
                }
            }
            else if(currentLine.equals("<GRAPHIC>")){
                currentIndex++;
                while (!(currentLine = singleDoc.get(currentIndex)).equals("</GRAPHIC>")){
                    if (!(currentLine.equals("<P>") || currentLine.equals("</P>"))){
                        docText = docText.concat(currentLine).concat("\n");
                    }
                    currentIndex++;
                }
            }
        }

        docTokens = TextTokenizer.splitWords(docText);
        docTokenSize = docTokens.size();

        String[] months = {"JANUARY ", "FEBRUARY ", "MARCH ", "APRIL ", "MAY ", "JUNE ", "JULY ",
                "AUGUST ", "SEPTEMBER ", "OCTOBER ", "NOVEMBER ", "DECEMBER "};

        String docDay = docNo.substring(4, 6);
        int docMonthIndex = Integer.parseInt(docNo.substring(2, 4)) - 1;
        String docYear = "19".concat(docNo.substring(6, 8));

        String docDate = months[docMonthIndex].concat(docDay).concat(", ").concat(docYear);

        metaData.add("docno: ".concat(docNo));
        metaData.add("internal id: ".concat(INTERNAL_ID.toString()));
        metaData.add("date: ".concat(docDate));
        metaData.add("headline: ".concat(docHead));

        String newPath = storePath.concat("/").concat(docYear).concat("/")
                .concat(months[docMonthIndex].trim()).concat("/").concat(docDay);
        String metaPath = storePath.concat("/meta-data");
        String lengthPath = storePath.concat("/length.txt");
        String lexiconPath = storePath.concat("/lexicon.txt");

        createRowDocFile(newPath, docNo, singleDoc);
        createMetaFile(metaPath, metaData);
        updateMapper(docNo);
        updateLength(lengthPath, docTokenSize);

        for (String term: docTokens) {
            updateLexicon(term);
            updateWordCount(term);
        }
        updatePosting();

        INTERNAL_ID++;
    }


    private static void updateLexicon (String term) {
        if (!LEXICON.containsKey(term)) {
            int lexicon_tale = LEXICON.size() + 1;
            LEXICON.put(term, lexicon_tale);
        }
    }

    private static void outputLexicon (String storePath) {
        String lexiconPath = storePath.concat("/lexicon.txt");
        String lexiconLine = "";
        Integer lexiconValue;
        File lexiconFile = new File(lexiconPath);

        try{
            if(!lexiconFile.exists()){
                lexiconFile.createNewFile();
            }
            try (OutputStream lexiconOutput = new FileOutputStream(lexiconPath, true)) {
                for (String lexiconKey: LEXICON.keySet()){
                    lexiconValue = LEXICON.get(lexiconKey);
                    lexiconLine = lexiconLine.concat(lexiconKey).concat("=")
                            .concat(lexiconValue.toString()).concat("\n");
                    lexiconOutput.write(lexiconLine.getBytes(StandardCharsets.UTF_8));
                    lexiconOutput.flush();
                    lexiconLine = "";
                }
            }
        }
        catch (IOException e){
            System.out.println("Error: IO Exception with lexicon.txt.");
        }
    }

    private static void updateWordCount (String term) {
        int termId = LEXICON.get(term);
        int termCounts;

        if (WORDCOUNT.containsKey(termId)) {
            termCounts = WORDCOUNT.get(termId);
            termCounts++;
            WORDCOUNT.replace(termId, termCounts);
        }
        else {
            WORDCOUNT.put(termId, 1);
        }
    }


    private static void updatePosting () {
        ArrayList<Integer> termFrequency = new ArrayList<>();

        for (int termId: WORDCOUNT.keySet()) {
            if (POSTING.containsKey(termId)){
                termFrequency = POSTING.get(termId);
            }
            termFrequency.add(INTERNAL_ID);
            termFrequency.add(WORDCOUNT.get(termId));
            POSTING.put(termId, termFrequency);

            termFrequency = new ArrayList<>();
        }

        WORDCOUNT.clear();
    }

    private static void outputPosting (String storePath){
        String postingPath = storePath.concat("/posting.txt");
        List<Integer> singlePosting;
        File postingFile = new File(postingPath);

        try{
            if(!postingFile.exists()){
                postingFile.createNewFile();
            }
            try(OutputStream postingOutput = new FileOutputStream(postingPath, true)){
                try(BufferedWriter postingWriter = new BufferedWriter(new OutputStreamWriter(postingOutput))){
                    Set<Integer> postingKeySet = POSTING.keySet();
                    for (int key: postingKeySet){
                        postingWriter.write(String.valueOf(key));
                        postingWriter.newLine();

                        singlePosting = POSTING.get(key);
                        for (int value: singlePosting){
                            postingWriter.write(value + " ");
                        }
                        postingWriter.newLine();
                        postingWriter.flush();
                    }
                }
            }
        }
        catch (IOException e){
            System.out.println("Error: IO Exception with posting.txt.");
        }
    }

    private static void updateLength (String lengthPath, int docLength) {
        File lengthFile = new File(lengthPath);
        try{
            if(!lengthFile.exists()){
                lengthFile.createNewFile();
            }

            // every length variable account for 6 bytes in the file
            String docLengthString = String.format("%8d ", docLength);
            try (OutputStream lengthOutput = new FileOutputStream(lengthPath, true)) {
                lengthOutput.write(docLengthString.getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (IOException e){
            System.out.println("Error: Invalid format.");
        }
    }

    private static void outputMapper (String storePath) {
        String mapperPath = storePath.concat("/mapper.txt");
        String mapperValue = "";
        String mapperLine = "";

        File mapperFile = new File(mapperPath);
        try{
            if(!mapperFile.exists()){
                mapperFile.createNewFile();
            }

            try (OutputStream mapperOutput = new FileOutputStream(mapperPath, true)) {
                for (String mapperKey: MAPPER.keySet()) {
                    mapperValue = MAPPER.get(mapperKey);
                    mapperLine = mapperLine.concat(mapperKey).concat("=")
                            .concat(mapperValue).concat("\n");
                    mapperOutput.write(mapperLine.getBytes(StandardCharsets.UTF_8));
                    mapperOutput.flush();
                    mapperLine = "";
                }
            }
        }
        catch (IOException e){
            System.out.println("Error: IO Exception with mapper.txt");
        }
    }

    private static void updateMapper (String docNo) {
        String internalID_string = INTERNAL_ID.toString();
        if(!MAPPER.containsKey(docNo)){
            MAPPER.put(docNo, internalID_string);
        }
    }

    private static void createMetaFile (String metaPath, List<String> metaData) throws IOException{
        String metaPathSubLowerDir = String.valueOf(INTERNAL_ID / 1000 * 1000);
        String metaPathSubHigherDir = String.valueOf((INTERNAL_ID / 1000 + 1) * 1000 - 1);
        String newMetaPath = metaPath.concat("/").concat(metaPathSubLowerDir).concat("-")
                .concat(metaPathSubHigherDir);
        File metaFile = new File(newMetaPath);
        if(!metaFile.exists()){
            metaFile.mkdirs();
        }

        String newMetaFilePath = newMetaPath.concat("/").concat(INTERNAL_ID.toString())
                .concat(".txt");

        metaFile = new File(newMetaFilePath);
        metaFile.createNewFile();
        try (OutputStream outputMeta = new FileOutputStream(newMetaFilePath)) {
            for (String singleMetaLine: metaData) {
                outputMeta.write(singleMetaLine.getBytes(StandardCharsets.UTF_8));
                outputMeta.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static void createRowDocFile(String newPath, String docNo, List<String> singleDoc) throws IOException{
        File docFile = new File(newPath);
        if (!docFile.exists()){
            docFile.mkdirs();
        }

        String fileNewPath = newPath.concat("/").concat(docNo).concat(".xml");

        docFile = new File(fileNewPath);
        docFile.createNewFile();

        try (OutputStream outputDoc = new FileOutputStream(fileNewPath)) {
            for (String singleDocLine : singleDoc) {
                outputDoc.write(singleDocLine.getBytes(StandardCharsets.UTF_8));
                outputDoc.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}

