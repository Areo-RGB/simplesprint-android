import {
  formatAcceleration,
  formatDurationNanos,
  formatMeters,
  formatSpeedWithUnit,
} from "../utils";
import type { ControlPath, MonitoringPointRow } from "../api/types";
import ActionButton from "./ActionButton";

type RaceTimerPanelProps = {
  raceClockDisplay: string;
  timerStateLabel: string;
  activeTab: string;
  onOpenSavedResults: () => void;
  busyAction: string;
  postControl: (path: ControlPath, body?: unknown, actionKey?: string) => Promise<unknown>;
  canStartMonitoring: boolean;
  monitoringActive: boolean;
  saveResultsJson: () => void;
  canSaveResults: boolean;
  monitoringPointRows: MonitoringPointRow[];
  speedUnit: string;
  toggleSpeedUnit: () => void;
  mergeWithHeader?: boolean;
  withFloatingTabs?: boolean;
};

export default function RaceTimerPanel({
  raceClockDisplay,
  timerStateLabel,
  activeTab,
  onOpenSavedResults,
  busyAction,
  postControl,
  canStartMonitoring,
  monitoringActive,
  saveResultsJson,
  canSaveResults,
  monitoringPointRows,
  speedUnit,
  toggleSpeedUnit,
  mergeWithHeader = false,
  withFloatingTabs = false,
}: RaceTimerPanelProps) {
  const stateIsReady = timerStateLabel === "Ready";
  const stateIsMonitoring = timerStateLabel === "Monitoring";

  return (
    <section className={withFloatingTabs ? "pt-20" : ""}>
      <div
        className={`overflow-hidden border-[2px] border-black bg-white shadow-[1px_1px_0px_0px_rgba(0,0,0,1)] ${
          mergeWithHeader ? "border-t-0" : ""
        }`}
      >
          <div className="flex flex-wrap items-center justify-between gap-2 border-b-[2px] border-black px-3 py-2.5">
            <p className="text-xs font-bold uppercase tracking-[0.22em] text-black sm:text-sm">Race Timer</p>
            <nav className="inline-flex items-center gap-1 rounded-sm border-[2px] border-black bg-[#f4f4f0] p-0.5 shadow-[1px_1px_0px_0px_rgba(0,0,0,1)]">
              <button
                type="button"
                className={`px-3 py-1.5 text-[11px] font-bold uppercase tracking-[0.18em] transition-colors sm:px-4 sm:text-xs ${
                  activeTab === "live" ? "bg-black text-[#FFEA00]" : "text-black hover:bg-gray-100"
                }`}
              >
                Live Monitor
              </button>
              <button
                type="button"
                onClick={onOpenSavedResults}
                className={`px-3 py-1.5 text-[11px] font-bold uppercase tracking-[0.18em] transition-colors sm:px-4 sm:text-xs ${
                  activeTab === "saved" ? "bg-black text-[#FFEA00]" : "text-black hover:bg-gray-100"
                }`}
              >
                Saved Results
              </button>
            </nav>
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.14em] text-black sm:text-sm">
              <span className="relative flex h-4 w-4 items-center justify-center border-2 border-black bg-white">
                {stateIsMonitoring && (
                  <span className="absolute inline-flex h-full w-full animate-ping border-2 border-black bg-[#00E676] opacity-55"></span>
                )}
                <span
                  className={`relative inline-flex h-2.5 w-2.5 border-[1.5px] border-black ${
                    stateIsMonitoring ? "bg-[#00E676]" : stateIsReady ? "bg-white" : "bg-gray-300"
                  }`}
                ></span>
              </span>
              {timerStateLabel}
            </div>
          </div>

          <div className="px-3 pb-3">
            <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2">
              <div className="space-y-3">
                <div className="relative flex items-center justify-center border-[2px] border-black bg-[#f8f8f5] py-8 shadow-[inset_2px_2px_0px_0px_rgba(0,0,0,0.1)] overflow-hidden">
                  <div className="absolute inset-0 bg-[linear-gradient(to_right,#000_1px,transparent_1px),linear-gradient(to_bottom,#000_1px,transparent_1px)] bg-[size:1.5rem_1.5rem] opacity-[0.04]"></div>
                  <p className="relative font-mono text-5xl font-black leading-none tracking-tight text-black sm:text-6xl md:text-7xl">
                    {raceClockDisplay}
                  </p>
                </div>

                <div className="flex flex-wrap items-center justify-center gap-2 border-[2px] border-black bg-white p-2">
                  <ActionButton
                    label={monitoringActive ? "Stop Monitoring" : "Start Monitoring"}
                    onClick={() => {
                      void postControl(monitoringActive ? "/api/control/stop-monitoring" : "/api/control/start-monitoring");
                    }}
                    busy={busyAction === "/api/control/start-monitoring" || busyAction === "/api/control/stop-monitoring"}
                    disabled={monitoringActive ? false : !canStartMonitoring}
                    variant={monitoringActive ? "stop" : "start"}
                    active={monitoringActive}
                  />
                  <ActionButton
                    label="Reset Run"
                    onClick={() => {
                      void postControl("/api/control/reset-run");
                    }}
                    busy={busyAction === "/api/control/reset-run"}
                    variant="secondary"
                  />
                  <ActionButton
                    label="Save Results JSON"
                    onClick={saveResultsJson}
                    busy={busyAction === "/api/control/save-results"}
                    disabled={!canSaveResults}
                    variant="secondary"
                  />
                </div>
              </div>

              <div className="min-w-0 md:h-full">
                {monitoringPointRows.length === 0 ? (
                  <p className="border-[2px] border-black bg-white p-4 text-xs font-bold uppercase tracking-wide text-gray-500 sm:text-sm md:flex md:h-full md:items-center">
                    No monitoring results recorded yet.
                  </p>
                ) : (
                  <div className="h-full overflow-auto border-[2px] border-black">
                    <table className="min-w-full text-left text-xs sm:text-sm md:h-full">
                      <thead className="border-b-[2px] border-black bg-[#FFEA00] text-[11px] font-bold uppercase tracking-[0.16em] text-black">
                        <tr>
                          <th className="border-r-[2px] border-black p-2">Distance</th>
                          <th className="border-r-[2px] border-black p-2">Time</th>
                          <th className="border-r-[2px] border-black p-2">Speed</th>
                          <th className="p-2">Acceleration (m/s^2)</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y-[2px] divide-black bg-white">
                        {monitoringPointRows.map(({ lap, pointSpeedMps, accelerationMps2 }) => {
                          return (
                            <tr key={lap.id}>
                              <td className="border-r-[2px] border-black p-2 font-bold uppercase text-black">{formatMeters(lap.distanceMeters)}</td>
                              <td className="border-r-[2px] border-black p-2 font-mono font-bold text-black">{formatDurationNanos(lap.elapsedNanos)}</td>
                              <td className="border-r-[2px] border-black p-2 font-bold text-black">
                                <button type="button" onClick={toggleSpeedUnit} className="font-mono transition-colors hover:text-[#FF1744]">
                                  {formatSpeedWithUnit(pointSpeedMps ?? 0, speedUnit)}
                                </button>
                              </td>
                              <td className="p-2 font-bold text-black">{formatAcceleration(accelerationMps2 ?? 0)}</td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          </div>
      </div>
    </section>
  );
}
