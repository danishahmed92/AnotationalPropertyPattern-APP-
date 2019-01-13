package datacollection;

import org.elasticsearch.action.get.GetResponse;
import properties.PropertyUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author DANISH AHMED on 1/10/2019
 */
public class TripleSentenceCollector extends DataStorage {
    private ElasticSearch elasticSearch = new ElasticSearch();

    public String getCandidateSentences(HashMap<String, String> tripleDataMap) {
        String subUri = tripleDataMap.get("subUri");
        String objUri = tripleDataMap.get("objUri");
        String subLabel = tripleDataMap.get("subLabel");
        String objLabel = tripleDataMap.get("objLabel");

        String[] subArticleSentences = getSentencesOfArticle(getArticleForURI(subUri));
        String[] objArticleSentences = getSentencesOfArticle(getArticleForURI(objUri));
        String sentencesCollectedFromSubjArticle = null;
        String sentencesCollectedFromObjArticle = null;
        String sentence = null;
        if (subArticleSentences != null)
            sentencesCollectedFromSubjArticle = getSentencesHavingLabels(subLabel, objLabel, subArticleSentences);

        if (objArticleSentences != null)
            sentencesCollectedFromObjArticle = getSentencesHavingLabels(subLabel, objLabel, objArticleSentences);

        if (sentencesCollectedFromSubjArticle != null && sentencesCollectedFromObjArticle != null) {
            if (sentencesCollectedFromSubjArticle.length() <= sentencesCollectedFromObjArticle.length())
                sentence = sentencesCollectedFromSubjArticle;
            else
                sentence = sentencesCollectedFromObjArticle;
        } else if (sentencesCollectedFromSubjArticle != null) {
            sentence = sentencesCollectedFromSubjArticle;
        } else {
            sentence = sentencesCollectedFromObjArticle;
        }
        return sentence;
    }

    private String getSentencesHavingLabels(String subLabel, String objLabel, String[] articleSentences) {
        Boolean foundSubj = false;
        Boolean foundObj = false;

        List<String> sentences = new LinkedList<>();
        int index = 0;
        for (String sentence : articleSentences) {
            if (!foundSubj)
                foundSubj = sentenceContainsString(sentence, subLabel);

            if (!foundObj)
                foundObj = sentenceContainsString(sentence, objLabel);

            if (foundSubj && foundObj) {
                sentence = filterURLFromSentence(sentence);
                sentences.add(sentence);

                // look if next sentences has either subj or obj
                sentences.addAll(getExtendedSentences(new LinkedList<>(), subLabel, objLabel, articleSentences, index + 1));
                break;
            } else if (foundSubj || foundObj) {
                sentence = filterURLFromSentence(sentence);
                sentences.add(sentence);
            }
            index++;
        }

        if (!foundSubj || !foundObj)
            return null;
        return (String.join(". ", sentences) + ".");
    }

    public String filterURLFromSentence(String sentence) {
        sentence = sentence.replaceAll("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,4}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*\\s)", "");
        sentence = sentence.replaceAll("[\\[\\]]","");
        return sentence;
    }

    private List<String> getExtendedSentences(List<String> sentences, String subLabel, String objLabel, String[] articleSentences, int startIndex) {
        if (startIndex == articleSentences.length)
            return sentences;

        Boolean foundSubj = sentenceContainsString(articleSentences[startIndex], subLabel);
        Boolean foundObj = sentenceContainsString(articleSentences[startIndex], objLabel);
        if (foundSubj ^ foundObj) {
            String sentence = filterURLFromSentence(articleSentences[startIndex]);
            sentences.add(sentence);
            return getExtendedSentences(sentences, subLabel, objLabel, articleSentences, startIndex + 1);
        } else
            return sentences;
    }

    private Boolean sentenceContainsString(String sentence, String str) {
        return sentence.contains(str);
    }

    private String getArticleForURI(String uri) {
        GetResponse response = null;
        try {
            response = elasticSearch.getDocumentById(uri);
            return String.valueOf(response.getSource().get("text"));
        } catch (NullPointerException e) {
            System.out.print("No article found for URI:\t" + uri);
            e.printStackTrace();
            return null;
        }
    }

    private String[] getSentencesOfArticle(String article) {
        if (article == null)
            return null;
        return article.split("\\.\\s|\\s\\*\\s");
    }

    public void getAndStoreSentencesForAllPropertyTriples() {
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            for (String property : properties) {
                HashMap<Integer, HashMap<String, String>> tripleIdDataMap = PropertyUtils.getTriplesForProperty(property);
                for (Integer tripleId : tripleIdDataMap.keySet()) {
                    HashMap<String, String> tripleDataMap = tripleIdDataMap.get(tripleId);
                    String tripleSentence = getCandidateSentences(tripleDataMap);
                    if (tripleSentence != null) {
                        insertSentenceToDB(tripleId, property, tripleSentence);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void narrowAllSentencesByLastOccurrenceAndUpdate() {
        try {
            List<String> properties = PropertyUtils.getAllProperties();
            for (String property : properties) {
                HashMap<Integer, HashMap<String, String>> sentenceTripleDataMap = PropertyUtils.getSentencesForProperty(property);
                for (Integer sentenceId : sentenceTripleDataMap.keySet()) {
                    String subLabel = sentenceTripleDataMap.get(sentenceId).get("subLabel");
                    String objLabel = sentenceTripleDataMap.get(sentenceId).get("objLabel");
                    String sentence = sentenceTripleDataMap.get(sentenceId).get("sentence");

                    String newSentence = narrowSentencesByLastOccurrence(sentence, subLabel, objLabel);
                    if (!newSentence.equals(sentence)) {
                        // replace sentence in DB
                        System.out.println(sentenceId);
                        updateTripleSentence(sentenceId, newSentence);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String narrowSentencesByLastOccurrence(String sentence, String subLabel, String objLabel) {
        int subjLastIndex = sentence.lastIndexOf(subLabel);
        int objLastIndex = sentence.lastIndexOf(objLabel);

        int startIndex = -1;
        if (subjLastIndex < objLastIndex)
            startIndex = subjLastIndex;
        else
            startIndex = objLastIndex;

        if (startIndex == 0)
            return sentence;

        String sentenceBeforeOccurrence = sentence.substring(0, startIndex);
        String[] prevSentences = getSentencesOfArticle(sentenceBeforeOccurrence);

        if (prevSentences.length > 1) {
            String startSentence = prevSentences[prevSentences.length - 1];
            return startSentence + sentence.substring(startIndex, sentence.length());
        } else if (prevSentences.length == 1){
            String lastChar = prevSentences[0].substring(prevSentences[0].length() - 1);
            String newSentence;
            if (lastChar.equals(" "))
                newSentence = prevSentences[0] + sentence.substring(startIndex, sentence.length());
            else
                newSentence = sentence.substring(startIndex, sentence.length());
            return newSentence;
        }
        return sentence;
    }

    public static void main(String[] args) {
        TripleSentenceCollector tsc = new TripleSentenceCollector();
//        tsc.getAndStoreSentencesForAllPropertyTriples();
        tsc.narrowAllSentencesByLastOccurrenceAndUpdate();
    }
}
