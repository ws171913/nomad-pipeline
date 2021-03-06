/*
 * The MIT License
 *
 * Copyright 2019 jballet.
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
package info.multani.jenkins.plugins.nomad;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import hudson.security.ACL;
import java.io.IOException;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

/**
 * Create a new Nomad client.
 *
 * @author jballet
 */
public class NomadClientFactory {

    private final String url;

    private final StandardCredentials credentials;

    public NomadClientFactory(String url, String credentials) {
        this.url = url;
        this.credentials = credentials != null ? getCredentials(credentials) : null;
    }

    public NomadApiClient createClient() throws IOException {
        NomadApiConfiguration.Builder builder = new NomadApiConfiguration.Builder();
        builder.setAddress(url);

        if (credentials instanceof StringCredentials) {
            String token = ((StringCredentials) credentials).getSecret().getPlainText();
            builder.setAuthToken(token);
        }

        NomadApiConfiguration config = builder.build();
        NomadApiClient client = new NomadApiClient(config);
        return client;
    }

    private StandardCredentials getCredentials(String credentials) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()
                ),
                CredentialsMatchers.withId(credentials)
        );
    }
}
