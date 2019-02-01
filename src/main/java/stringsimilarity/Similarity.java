package stringsimilarity;

import org.apache.commons.text.similarity.JaroWinklerDistance;

/**
 * @author DANISH AHMED on 1/31/2019
 */
public class Similarity {
    public static void main(String[] args) {
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        Double similarity = jaroWinklerDistance.apply("{(VBD)-nmod:to>(NN)-nmod:of>%D%(NNP)}respond{(VBD)-nsubj>(NNP)}",
                "{(VBD)-nsubj>(NN)-nmod:of>%D%(NNP)}respond{(VBD)-nmod:to>(NNP)}");

        System.out.println(similarity);
    }
}
