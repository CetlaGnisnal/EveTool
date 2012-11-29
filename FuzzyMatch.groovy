@Grab(group='org.apache.commons', module='commons-lang3', version='3.1')
@Grab(group='commons-codec', module='commons-codec', version='1.7')

import groovy.transform.Immutable
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import org.apache.commons.codec.language.*
import org.apache.commons.lang3.StringUtils

@Immutable
class FuzzyMatch<T> {

    static final RefinedSoundex soundex = new RefinedSoundex()

    Collection<T> corpus
    Closure matchOn = { return it }

    Collection<T> findAll(String specimin) {
        def soundPass = pick(sounds(specimin, corpus), DefaultGroovyMethods.&max)
        def distancePass = take(10, distances(specimin, soundPass).sort {a, b-> a.value <=> b.value  }.keySet().toList())
        return take(5, distancePass)
    }

    private Map<T, Integer> distances(String specimin, Collection<T> corpus) {
        corpus.collectEntries { [(it): distance(specimin, matchOn(it))] }
    }

    private int distance(String a, String b) {
        // Lower is better.
        StringUtils.getLevenshteinDistance((java.lang.CharSequence) a, (java.lang.CharSequence) b)
    }

    private Map<T, Integer> sounds(String specimin, Collection<T> corpus) {
        corpus.collectEntries { [(it): sound(specimin, matchOn(it))] }
    }

    private int sound(String a, String b) {
        // Higher is better.
        soundex.difference(a, b)
    }

    private Collection<T> pick(Map<T, Integer> computed, Closure f) {
        Integer i = f(computed.values())
        computed.findAll {k,v-> v == i}.keySet().toList()
    }

    private Collection<T> take(Integer i, Collection<T> c) {
        if(!c) return c

        def size = c.size()
        c[0..(size < i ? size - 1 : i - 1)]
    }

}
