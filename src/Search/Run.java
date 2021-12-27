package Search;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Run {

    /******************
     * Static Variables
     ******************* */

    static Map<Integer, String> POSTING = new HashMap<>();

    /*
     * HashMap for lexicon
     * key = term
     * value = term id
     * */
    public static Map<String, Integer> lexiconMap = new HashMap<>();

    /*
     * LinkedHashMaps for LA-only.trec8-401.450.minus416-423-437-444-447.txt
     * key = topic id
     * value = List of relevant documents' docNo
     * */
    public static Map<Integer, List<String>> relDocs = new LinkedHashMap<>();

    /*
     * HashMap for size of relevant documents
     * key = topic id
     * value = size of relevant documents
     * */
    public static Map<Integer, Integer> relSize = new HashMap<>();

    /*
     * HashMap for length of documents
     * key = docNo
     * value = length of the document
     * */
    public static Map<String, Integer> lengthMap = new HashMap<>();

    /*
     * HashMap for mapper
     * key = query id
     * value = query content
     * */
    public static Map<Integer, String> mapper = new HashMap<>();

    // total number of documents in the collection
    public static final int N = 131896;
    // documents' average length in the collection
    public static double avdl = 0;

    /***********************
     * Program Entry Point
     ************************ */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("////////////////////////////////////////////////////////////////////////////////////////");
            System.out.println("Error: invalid command, please follow commands below to execute the program.");
            System.out.println("java Search.Run <latime_path>");
            System.out.println("////////////////////////////////////////////////////////////////////////////////////////");
            System.exit(1);
        }
        String latimesDatabase = args[0];

//        String latimesDatabase = "D:\\UWaterloo\\MSCI_720\\A5\\latimesDatabase";

        if (!latimesDatabase.endsWith("\\") && !latimesDatabase.endsWith("/")) {
            latimesDatabase = latimesDatabase.concat("/");
        }
        String postingFilePath = latimesDatabase.concat("posting.txt");
        String lengthFilePath = latimesDatabase.concat("length.txt");
        String mapperFilePath = latimesDatabase.concat("mapper.txt");
        String lexiconFilePath = latimesDatabase.concat("lexicon.txt");

        readPosting(postingFilePath);
        readLexicon(lexiconFilePath);
        readDocLength(lengthFilePath, mapperFilePath);

        // key: docNo, value: BM25 score
        Map<String, Double> bmScore;

        Scanner scanner = new Scanner(System.in);
        Calendar calendar;
        String query, command;
        String rowDocument, headline, date;
        String[] rowDocumentArray = new String[10];
        List<List<String>> docSentences;
        List<String> snippets;
        int rankId, headlineSize;

        while (true) {
            System.out.print("\nEnter your query: ");
            query = scanner.nextLine();

            calendar = Calendar.getInstance();
            long startTime = calendar.getTime().getTime();
            System.out.println("Processing query...");
            bmScore = calcBmScore(query);
            System.out.println("<Search Result>");
            System.out.println("`````````````````````````````````````````````````````````````````````");
            if (bmScore.isEmpty()) {
                System.out.println("There is no result for the given query.");
                System.out.println("`````````````````````````````````````````````````````````````````````");
            }

            rankId = 1;
            for (Map.Entry<String, Double> scoreEntry: bmScore.entrySet()) {
                rowDocument = getRowByDocNo(latimesDatabase, scoreEntry.getKey());
                rowDocumentArray[rankId-1] = rowDocument;
                docSentences = Tokenizer.splitSentences(rowDocument);
                snippets = snippetProcessor(docSentences, query);
                headline = Tokenizer.parseHeadline(docSentences);
                headlineSize = headline.length();
                date = Tokenizer.parseDate(scoreEntry.getKey());

                System.out.print(rankId + ". ");
                for (int i = 1; i < headlineSize + 1; i++) {
                    System.out.print(headline.charAt(i - 1));
                    if (i % 60 == 0) {
                        System.out.print("\n");
                    }
                }
                System.out.println(date + '\n');
                System.out.print(snippets.get(0).trim().concat(". \n"));
                System.out.print(snippets.get(1).trim().concat(". ("+scoreEntry.getKey()+")\n\n"));
                System.out.println("`````````````````````````````````````````````````````````````````````");

                rankId++;
            }

            calendar = Calendar.getInstance();
            long endTime = calendar.getTime().getTime();
            System.out.println("Retrieval took " + ((endTime-startTime)*0.001) + " seconds.");
            System.out.println("------------------------------------------------------------------------------------");

            while (true) {
                System.out.print("Enter your command: ");
                command = scanner.nextLine();
                if (command.equals("Q") || command.equals("N")) {
                    System.out.println("------------------------------------------------------------------------------------");
                    break;
                } else {
                    try { rankId = Integer.parseInt(command); }
                    catch (NumberFormatException e) { continue; }
                    System.out.println(rowDocumentArray[rankId - 1]);
                }
                System.out.println("------------------------------------------------------------------------------------");
            }
            if (command.equals("Q")) { break; }
        }
        System.out.println("System exit.");
    }

    /****************
     * BM25 Algorithm
     ***************** */
    private static Map<String, Double> calcBmScore(String queryContent) {
        List<String> queryWords = Tokenizer.splitWords(queryContent);

        List<Integer> wordId = new ArrayList<>();
        Map<Integer, Integer> wordCount = new LinkedHashMap<>();
        List<int[]> postings = new ArrayList<>();
        Map<String, Double> bmScore;

        String postingTmp;
        String[] QWPosingString;

        int wordIdTmp, i = 0;
        for (String word: queryWords) {
            if (!lexiconMap.containsKey(word)) {
                continue;
            }
            wordIdTmp = lexiconMap.get(word);
            if (wordId.contains(wordIdTmp)) {
                wordCount.put(wordIdTmp, wordCount.get(wordIdTmp) + 1);
                continue;
            }

            wordId.add(wordIdTmp);
            wordCount.put(wordIdTmp, 1);
            postingTmp = POSTING.get(wordId.get(i));
            i++;

            QWPosingString = postingTmp.trim().split("\\s+");
            postings.add(Arrays.stream(QWPosingString).mapToInt(Integer::parseInt).toArray());
        }
        bmScore = BM25(postings, wordCount);

        return bmScore;
    }

    private static Map<String, Double> BM25(List<int[]> QWPostings, Map<Integer, Integer> wordCount) {
        double k1 = 1.2;
        double k2 = 7;
        double b = 0.75;
        Map<String, Double> bmScore = new LinkedHashMap<>();
        Collection<Integer> wordCountCollection = wordCount.values();
        Iterator<Integer> wordCountIterator = wordCountCollection.iterator();

        int ni, postingSize, tf;
        double currentBmScore, bmScoreTmp, K;
        Integer qtf;
        String docNo;
        for (int[] posting: QWPostings) {
            postingSize = posting.length;
            ni = (postingSize / 2);
            qtf = wordCountIterator.next();

            for (int i = 0; i < postingSize; i += 2) {
                tf = posting[i + 1];
                docNo = mapper.get(posting[i]);
                K = k1 * ((1 - b) + b * lengthMap.get(docNo) / avdl);
                currentBmScore = Math.log((N-ni+0.5)/(ni+0.5))*
                        ((k1+1)*tf/(K+tf))*((k2+1)*qtf/(k2+qtf));

                if (!bmScore.containsKey(docNo)) {
                    bmScore.put(docNo, currentBmScore);
                }
                else {
                    bmScoreTmp = bmScore.get(docNo);
                    bmScore.put(docNo, bmScoreTmp + currentBmScore);
                }
            }
        }
        bmScore = bmSort(bmScore);
        return bmScore;
    }

    private static Map<String, Double> bmSort (Map<String, Double> bmScore) {
        List<Map.Entry<String, Double>> list = new ArrayList<>(bmScore.entrySet());
        list.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        int i = 0;
        Map<String, Double> sortedResult = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry: list) {
            sortedResult.put(entry.getKey(), entry.getValue());
            i++;
            if (i == 10) { break; }
        }

        return sortedResult;
    }

    private static List<String> snippetProcessor(List<List<String>> sentenceList, String query) {
        int h, l, c, d, k, cTmp, kCount;
        int score, sentenceSize;

        List<String> queryWords = Tokenizer.splitWords(query);
        List<String> headingSentence = sentenceList.get(0);
        List<String> textSentence = sentenceList.get(1);
        List<String> sentenceWords;
        Map<String, Integer> sentenceScore = new LinkedHashMap<>();
        int querySize = queryWords.size();

        // heading sentence score calculation
        for (String sentence: headingSentence) {
            h = 1; c = 0; d = 0; k = 0;

            // getting c and d
            sentenceWords = Tokenizer.splitWords(sentence);
            for (String qWord: queryWords) {
                cTmp = c;
                for (String sWord: sentenceWords) {
                    if (qWord.equals(sWord)) { c++; }
                }
                if (c > cTmp) { d++; }
            }

            sentenceSize = sentenceWords.size();
            for (int i = 0; i < sentenceSize; i++) {
                kCount = 0;
                if (sentenceWords.get(i).equals(queryWords.get(0))) {
                    for (int j = 0; j < querySize; j++) {
                        if (sentenceWords.get(i).equals(queryWords.get(j))) {
                            kCount++;
                            if (i < sentenceSize - 1) { i++; }
                            else { break; }
                        }
                        else { i--; break; }
                    }
                }
                k = Math.max(k, kCount);
            }
            score = h + c + d + k;
            sentenceScore.put(sentence, score);
        }

        // text sentence score calculation
        for (String sentence: textSentence) {
            l = 2; c = 0; d = 0; k = 0;

            // getting c and d
            sentenceWords = Tokenizer.splitWords(sentence);
            for (String qWord: queryWords) {
                cTmp = c;
                for (String sWord: sentenceWords) {
                    if (qWord.equals(sWord)) { c++; }
                }
                if (c > cTmp) { d++; }
            }

            sentenceSize = sentenceWords.size();
            for (int i = 0; i < sentenceSize; i++) {
                kCount = 0;
                if (sentenceWords.get(i).equals(queryWords.get(0))) {
                    for (int j = 0; j < querySize; j++) {
                        if (sentenceWords.get(i).equals(queryWords.get(j))) {
                            kCount++;
                            if (i < sentenceSize - 1) { i++; }
                            else { break; }
                        }
                        else { i--; break; }
                    }
                }
                k = Math.max(k, kCount);
            }
            score = l + c + d + k;
            sentenceScore.put(sentence, score);

            if (l != 0) { l--; }
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(sentenceScore.entrySet());
        list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        int i = 0;
        List<String> sortedResult = new ArrayList<>();
        for (Map.Entry<String, Integer> entry: list) {
            sortedResult.add(entry.getKey());
            i++;
            if (i == 2) { break; }
        }

        return sortedResult;
    }

    /************************
     * File Extracting Functions
     ************************* */
    private static String getRowByDocNo (String storagePath, String docNo) {
        String[] months = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY",
                "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};
        String docYear = "19".concat(docNo.substring(6, 8));
        String docMonth = months[Integer.parseInt(docNo.substring(2, 4)) - 1];
        String docDay = docNo.substring(4, 6);

        String rowDocPath = storagePath.concat("/").concat(docYear)
                .concat("/").concat(docMonth)
                .concat("/").concat(docDay)
                .concat("/").concat(docNo.trim()).concat(".xml");

        return wholeFileReader(rowDocPath);
    }

    private static String wholeFileReader (String path) {
        String file = "";
        try (InputStream metaFileInput = new FileInputStream(path)) {
            int inputBuff;
            StringBuilder sb = new StringBuilder();
            while ((inputBuff = metaFileInput.read()) != -1) {
                sb.append((char) inputBuff);
            }
            file = sb.toString();
        }
        catch (IOException e) {
            System.out.println("Error: cannot open files when reading row documents.");
        }
        return file;
    }

    /************************
     * File Reading Functions
     ************************* */
    private static void readPosting (String postingPath) {
        System.out.println("Reading inverted index into memory...");
        try {
            String postingString = new String(Files.readAllBytes(Paths.get(postingPath)));
            postingParser(postingString);
        }
        catch (IOException e) {
            System.out.println("Error: cannot open posting file.");
            System.exit(1);
        }
    }

    private static void postingParser (String postingString) {
        String[] postingGroups = postingString.split("[\n\r]+");
        int postingGroupSize = postingGroups.length;
        int termIdInt;

        for (int i = 0; i < postingGroupSize; i++) {
            termIdInt = Integer.parseInt(postingGroups[i]);
            i++;
            POSTING.put(termIdInt, postingGroups[i].trim());
        }
    }

    private static void readLexicon (String lexiconFilePath) {
        System.out.println("Reading lexicon into memory...");
        String lexiconTmp;

        try (InputStream lexiconInput = new FileInputStream(lexiconFilePath)) {
            try (BufferedReader lexiconReader = new BufferedReader(new InputStreamReader(lexiconInput))) {
                while ((lexiconTmp = lexiconReader.readLine()) != null) {
                    lexiconParser(lexiconTmp);
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error: cannot open lexicon file.");
            System.exit(1);
        }
    }

    private static void lexiconParser (String lexicon) {
        String[] singleLexicon = lexicon.split("=");
        lexiconMap.put(singleLexicon[0], Integer.valueOf(singleLexicon[1]));
    }

    private static void readDocLength (String lengthFilePath, String mapperFilePath) {
        System.out.println("Reading document lengths into memory...");
        String mapperLine, lengthLine;

        try {
            InputStream lengthInput = new FileInputStream(lengthFilePath);
            InputStream mapperInput = new FileInputStream(mapperFilePath);
            BufferedReader lengthReader = new BufferedReader(new InputStreamReader(lengthInput));
            BufferedReader mapperReader = new BufferedReader(new InputStreamReader(mapperInput));

            lengthLine = lengthReader.readLine().trim();
            String[] lengthGroup = lengthLine.split("\\s+");

            while ((mapperLine = mapperReader.readLine()) != null) {
                docLengthParser(mapperLine, lengthGroup);
            }
            avdl /= N;

            lengthReader.close();
            mapperReader.close();
            lengthInput.close();
            mapperInput.close();
        }
        catch (IOException e) {
            System.out.println("Error: cannot open length file / mapper file.");
            System.exit(1);
        }
    }

    private static void docLengthParser (String mapperLine, String[] lengthGroup) {
        String[] mapperElements = mapperLine.split("=");
        String docNo = mapperElements[0];
        int docIndex = Integer.parseInt(mapperElements[1]);
        int docLength = Integer.parseInt(lengthGroup[docIndex]);

        mapper.put(docIndex, docNo);
        lengthMap.put(docNo, docLength);
        avdl += docLength;
    }

}
