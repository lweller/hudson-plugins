/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.persona.simple;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.persona.Persona;
import hudson.plugins.persona.Quote;

import java.util.List;
import java.util.Random;

/**
 * Partial implementation of {@link Persona} that renders the plain text quote with an icon.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class SimplePersona extends Persona {
    private volatile List<String> quotes;
    private final Random random = new Random();

    /**
     * @param id
     *      Unique identifier of this persona.
     * @param quotes
     *      Collection of quotes.
     */
    protected SimplePersona(String id, List<String> quotes) {
        super(id);
        this.quotes = quotes;
    }

    protected void setQuotes(List<String> quotes) {
        this.quotes = quotes;
    }

    /**
     * Determines the icon and the background to render.
     */
    public abstract Image getImage(AbstractBuild<?, ?> build);

    public abstract Image getDefaultImage();

    @Override
    public Quote generateQuote(AbstractBuild<?, ?> build) {
        return new QuoteImpl(build,this, getRandomQuoteText());
    }

    public synchronized String getRandomQuoteText() {
        return quotes.get(random.nextInt(quotes.size()));
    }

    @Override
    public Quote generateProjectQuote(AbstractProject<?,?> project) {
        return new ProjectQuoteImpl(this,project);
    }
}
