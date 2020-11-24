import React from "react";

import "./WorkbenchApp.css";

import { persistentStore } from "../../../../carrotsearch/store/persistent-store.js";
import { view } from "@risingstack/react-easy-state";
import { clusterStore, searchResultStore } from "../../../store/services.js";
import { humanizeDuration } from "../../../../carrotsearch/lang/humanize.js";
import { Stats } from "../../../../carrotsearch/ui/Stats.js";
import { Views } from "../../../../carrotsearch/ui/Views.js";
import { clusterViews, resultsViews } from "../../../config-views.js";
import { Loading } from "../../../../carrotsearch/ui/Loading.js";
import { sources } from "../../../config-sources.js";
import { workbenchSourceStore } from "../store/source-store.js";
import { WorkbenchSide } from "./WorkbenchSide.js";
import { DottedStraightArrow } from "../../../../carrotsearch/ui/arrows/DottedStraightArrow.js";
import { DottedAngledArrow } from "../../../../carrotsearch/ui/arrows/DottedAngledArrow.js";
import { DottedArrowCurly } from "../../../../carrotsearch/ui/arrows/DottedArrowCurly.js";

const uiStore = persistentStore("workbench:ui", {
  clusterView: "folders"
});

const ResultStats = view(() => {
  const stats = [
    {
      id: "result-count",
      value: searchResultStore.searchResult.documents.length,
      label: "results"
    },
    {
      id: "cluster-count",
      value: clusterStore.clusters.length,
      label: "clusters"
    },
    {
      id: "clustered-docs",
      value: (100.0 * clusterStore.getClusteredDocsRatio()).toFixed(1) + "%",
      label: "clustered docs"
    },
    {
      id: "processing-time",
      value: humanizeDuration(clusterStore.serviceInfo?.clusteringTimeMillis),
      label: "clustering time"
    }
  ];

  return (
    <div className="stats">
      <Stats stats={stats} />
    </div>
  );
});

const SourceConfigurationStep = view(() => {
  const source = sources[workbenchSourceStore.source];
  const help = source.createIntroHelp?.();
  return (
    <li className="SourceConfiguration">
      <DottedAngledArrow />
      <h3>
        Configure <strong>{source.label}</strong> data source
      </h3>
      {help}
    </li>
  );
});

const WorkbenchIntroSteps = () => {
  return (
    <div className="WorkbenchIntroSteps">
      <ol>
        <li className="SourceAlgorithmChoice">
          <DottedStraightArrow />
          <h3>Choose data source and clustering algorithm</h3>
        </li>
        <SourceConfigurationStep />
        <li className="ButtonPress">
          <DottedArrowCurly />
          <h3>
            Press the <strong>Cluster</strong> button
          </h3>
        </li>
      </ol>
    </div>
  );
};

export const WorkbenchIntro = () => {
  return (
    <div className="WorkbenchMain WorkbenchIntro">
      <WorkbenchIntroSteps />
      <div className="WorkbenchIntroWelcome">
        <h2>
          This is Carrot<sup>2</sup> Clustering Workbench
        </h2>

        <ul>
          <li>clustering data from files, Solr, Elasticsearch</li>
          <li>experimenting with clustering parameters</li>
          <li>exporting results</li>
        </ul>
      </div>
    </div>
  );
};

const WorkbenchMain = view(() => {
  if (searchResultStore.initial) {
    return <WorkbenchIntro />;
  }

  return (
    <div className="WorkbenchMain">
      <ResultStats />
      <div className="clusters">
        <Views
          activeView={uiStore.clusterView}
          views={clusterViews}
          onViewChange={newView => (uiStore.clusterView = newView)}
        >
          <Loading isLoading={() => clusterStore.loading} />
        </Views>
      </div>
      <div className="docs">
        <Views
          views={resultsViews}
          activeView="list"
          onViewChange={() => {}}
          source={sources[workbenchSourceStore.source]}
        >
          <Loading isLoading={() => searchResultStore.loading} />
        </Views>
      </div>
    </div>
  );
});

export const WorkbenchApp = () => {
  return (
    <div className="WorkbenchApp">
      <WorkbenchSide />
      <WorkbenchMain />
    </div>
  );
};
