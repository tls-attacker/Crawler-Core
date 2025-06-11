/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Email server target list provider that extracts mail servers from popular domain rankings.
 *
 * <p>The TrancoEmailListProvider builds upon existing target list providers (typically Tranco
 * rankings) to discover and extract mail server hostnames through DNS MX record resolution. This
 * enables TLS scanning of email infrastructure associated with popular websites.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li><strong>MX Record Resolution</strong> - Queries DNS for mail exchange records
 *   <li><strong>Mail Server Discovery</strong> - Identifies email infrastructure for popular
 *       domains
 *   <li><strong>Duplicate Removal</strong> - Returns unique mail server hostnames only
 *   <li><strong>Provider Agnostic</strong> - Works with any ITargetListProvider implementation
 * </ul>
 *
 * <p><strong>Processing Pipeline:</strong>
 *
 * <ol>
 *   <li><strong>Domain Acquisition</strong> - Obtain domain list from configured provider
 *   <li><strong>Hostname Extraction</strong> - Parse domains from provider-specific format
 *   <li><strong>MX Query</strong> - Perform DNS MX record lookups for each domain
 *   <li><strong>Mail Server Extraction</strong> - Extract mail server hostnames from MX records
 *   <li><strong>Deduplication</strong> - Return unique mail server list
 * </ol>
 *
 * <p><strong>DNS Resolution:</strong> Uses Java's InitialDirContext to perform DNS queries for MX
 * records. Failed lookups are logged but don't prevent processing of other domains.
 *
 * <p><strong>Error Handling:</strong>
 *
 * <ul>
 *   <li><strong>Missing MX Records</strong> - Domains without mail servers are silently skipped
 *   <li><strong>DNS Failures</strong> - Individual lookup failures are logged and ignored
 *   <li><strong>Malformed Records</strong> - Invalid MX records are handled gracefully
 * </ul>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li><strong>Email Security Studies</strong> - TLS adoption in email infrastructure
 *   <li><strong>Mail Server Surveys</strong> - Protocol support across popular email services
 *   <li><strong>Vulnerability Research</strong> - Security assessment of email systems
 *   <li><strong>Performance Analysis</strong> - Email protocol performance evaluation
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * TrancoListProvider domains = new TrancoListProvider(10000);
 * TrancoEmailListProvider emailProvider = new TrancoEmailListProvider(domains);
 * List<String> mailServers = emailProvider.getTargetList();
 * // Returns mail servers for top 10,000 Tranco domains
 * }</pre>
 *
 * @see ITargetListProvider
 * @see TrancoListProvider
 * @see CruxListProvider
 */
public class TrancoEmailListProvider implements ITargetListProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ITargetListProvider trancoList;

    /**
     * Creates a new email list provider using the specified domain list provider.
     *
     * <p>The constructor configures the provider to use any ITargetListProvider implementation as
     * the source for domain names, which will be queried for MX records to discover associated mail
     * servers.
     *
     * @param trancoList the target list provider to obtain domains from for MX record lookup
     */
    public TrancoEmailListProvider(ITargetListProvider trancoList) {
        this.trancoList = trancoList;
    }

    @Override
    public List<String> getTargetList() {
        List<String> dnsList = new ArrayList<>();
        try {
            InitialDirContext iDirC = new InitialDirContext();
            List<String> hostList = new ArrayList<>(this.trancoList.getTargetList());
            LOGGER.info("Fetching MX Hosts");
            for (String hold : hostList) {
                String hostname = hold.substring(hold.lastIndexOf(',') + 1);
                try {
                    Attributes attributes =
                            iDirC.getAttributes("dns:/" + hostname, new String[] {"MX"});
                    Attribute attributeMX = attributes.get("MX");

                    if (attributeMX != null) {
                        for (int i = 0; i < attributeMX.size(); i++) {
                            String getMX = attributeMX.get(i).toString();
                            dnsList.add(getMX.substring(getMX.lastIndexOf(' ') + 1));
                        }
                    }
                } catch (NamingException e) {
                    LOGGER.error("No MX record found for host: {} with error {}", hostname, e);
                }
            }
        } catch (NamingException e) {
            LOGGER.error(e);
        }
        return dnsList.stream().distinct().collect(Collectors.toList());
    }
}
