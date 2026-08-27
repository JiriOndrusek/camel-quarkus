/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {LitElement, css, html} from 'lit';
import {JsonRpc} from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-column';
import '@vaadin/button';

export class QwcCqIngestPipelines extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .container { margin: 10px; }
        .hint { color: var(--lumo-secondary-text-color); font-size: 0.9em; margin-bottom: 10px; }
    `;

    static properties = {
        _pipelines: {state: true}
    };

    constructor() {
        super();
        this._pipelines = [];
    }

    connectedCallback() {
        super.connectedCallback();
        this._refresh();
    }

    _refresh() {
        this.jsonRpc.getPipelines().then(response => {
            this._pipelines = response.result;
        });
    }

    render() {
        return html`
            <div class="container">
                <div class="hint">One row per ingestion pipeline: its consumer, configuration
                    highlights and live counters. Counters reset with the application.</div>
                <vaadin-button theme="small" @click=${() => this._refresh()}>Refresh</vaadin-button>
                <vaadin-grid .items=${this._pipelines} theme="compact row-stripes" all-rows-visible>
                    <vaadin-grid-column header="Pipeline" path="name" auto-width flex-grow="0"></vaadin-grid-column>
                    <vaadin-grid-column header="Source" path="source"></vaadin-grid-column>
                    <vaadin-grid-column header="Status" path="status" auto-width flex-grow="0"></vaadin-grid-column>
                    <vaadin-grid-column header="Parser" path="parser" auto-width flex-grow="0"></vaadin-grid-column>
                    <vaadin-grid-column header="Register" path="register" auto-width flex-grow="0"></vaadin-grid-column>
                    <vaadin-grid-column header="Documents" path="documents" auto-width flex-grow="0"></vaadin-grid-column>
                    <vaadin-grid-column header="Segments" path="segments" auto-width flex-grow="0"></vaadin-grid-column>
                    <vaadin-grid-column header="Empty" path="empty" auto-width flex-grow="0"></vaadin-grid-column>
                    <vaadin-grid-column header="Skipped" path="skipped" auto-width flex-grow="0"></vaadin-grid-column>
                    <vaadin-grid-column header="Failures" path="failures" auto-width flex-grow="0"></vaadin-grid-column>
                </vaadin-grid>
            </div>`;
    }
}

customElements.define('qwc-cq-ingest-pipelines', QwcCqIngestPipelines);
