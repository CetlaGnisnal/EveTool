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
        def exactMatch = corpus.find { matchOn(it) == specimin }
        if (exactMatch) return [exactMatch]

        def pass = corpus
        pass = filterOnCapitals specimin, pass
        pass = pickMax sounds(specimin, pass)
        pass = pickMin distances(specimin, pass), 1
        return take(5, pass)
    }

    private Collection<T> filterOnCapitals(String specimin, Collection<T> corpus) {
        def tokens = specimin.split(' ')
        tokens = tokens.collect { it[0].toLowerCase() }
        corpus.findAll { matchOn(it).toLowerCase() ==~ '^' + tokens.join(/.*\s/) + '.*' }
    }

    private Map<T, Integer> distances(String specimin, Collection<T> corpus) {
        corpus.collectEntries { [(it): distance(specimin, matchOn(it))] }
    }

    private int distance(String a, String b) {
        // Lower is better.
        StringUtils.getLevenshteinDistance((java.lang.CharSequence) a.toLowerCase(), (java.lang.CharSequence) b.toLowerCase())
    }

    private Map<T, Integer> sounds(String specimin, Collection<T> corpus) {
        corpus.collectEntries {
            [(it): sound(specimin, matchOn(it))]
        }
    }

    private int sound(String a, String b) {
        // Higher is better.
        soundex.difference(a, b)
    }

    private Collection<T> pickMin(Map<T, Integer> computed, Integer threshold = null) {
        _pick(computed, DefaultGroovyMethods.&min, {i, k, v-> threshold ? v < i + threshold || v == i : v == i})
    }

    private Collection<T> pickMax(Map<T, Integer> computed, Integer threshold = null) {
        _pick(computed, DefaultGroovyMethods.&max, {i, k, v-> threshold ? v > i - threshold || v == i : v == i})
    }

    private Collection<T> _pick(Map<T, Integer> computed, Closure f, Closure t) {
        Integer i = f(computed.values())
        computed.findAll(t.curry(i)).keySet().toList()
    }

    private Collection<T> take(Integer i, Collection<T> c) {
        if(!c) return c

        def size = c.size()
        c[0..(size < i ? size - 1 : i - 1)]
    }

}
