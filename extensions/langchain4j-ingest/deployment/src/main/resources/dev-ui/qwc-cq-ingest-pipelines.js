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
        h3 { margin-bottom: 5px; }
        .hint { color: var(--lumo-secondary-text-color); font-size: 0.9em; margin-bottom: 10px; }
    `;

    static properties = {
        _pipelines: {state: true},
        _documents: {state: true},
        _selected: {state: true}
    };

    constructor() {
        super();
        this._pipelines = [];
        this._documents = null;
        this._selected = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._refresh();
    }

    _refresh() {
        this.jsonRpc.getPipelines().then(response => {
            this._pipelines = response.result;
        });
        if (this._selected) {
            this._loadDocuments(this._selected);
        }
    }

    _loadDocuments(pipeline) {
        this._selected = pipeline;
        this.jsonRpc.getDocuments({pipeline: pipeline}).then(response => {
            this._documents = response.result;
        });
    }

    render() {
        return html`
            <div class="container">
                <h3>Ingestion pipelines</h3>
                <div class="hint">Select a sync pipeline to inspect its tracker — the answer to
                    "why doesn't my assistant know about document X".</div>
                <vaadin-button theme="small" @click=${() => this._refresh()}>Refresh</vaadin-button>
                <vaadin-grid .items=${this._pipelines}
                        @active-item-changed=${event => {
                            const item = event.detail.value;
                            if (item) this._loadDocuments(item.name);
                        }}>
                    <vaadin-grid-column path="name" header="Pipeline"></vaadin-grid-column>
                    <vaadin-grid-column path="mode" header="Mode"></vaadin-grid-column>
                    <vaadin-grid-column path="ready" header="Ready"></vaadin-grid-column>
                    <vaadin-grid-column path="documents" header="Documents"></vaadin-grid-column>
                    <vaadin-grid-column path="segments" header="Segments"></vaadin-grid-column>
                    <vaadin-grid-column path="replaced" header="Replaced"></vaadin-grid-column>
                    <vaadin-grid-column path="skippedUnchanged" header="Unchanged"></vaadin-grid-column>
                    <vaadin-grid-column path="deleted" header="Deleted"></vaadin-grid-column>
                    <vaadin-grid-column path="deadLettered" header="Dead-lettered"></vaadin-grid-column>
                    <vaadin-grid-column path="failures" header="Failures"></vaadin-grid-column>
                </vaadin-grid>
                ${this._documents ? html`
                    <h3>Tracker of '${this._selected}'</h3>
                    <vaadin-grid .items=${this._documents}>
                        <vaadin-grid-column path="documentId" header="Document"></vaadin-grid-column>
                        <vaadin-grid-column path="status" header="Status"></vaadin-grid-column>
                        <vaadin-grid-column path="segments" header="Segments"></vaadin-grid-column>
                        <vaadin-grid-column path="origin" header="Origin"></vaadin-grid-column>
                        <vaadin-grid-column path="tombstone" header="Tombstoned"></vaadin-grid-column>
                        <vaadin-grid-column path="pinned" header="Pinned"></vaadin-grid-column>
                    </vaadin-grid>` : html``}
            </div>`;
    }
}

customElements.define('qwc-cq-ingest-pipelines', QwcCqIngestPipelines);
