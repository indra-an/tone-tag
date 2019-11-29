declare module "@capacitor/core" {
  interface PluginRegistry {
    ToneTag: ToneTagPlugin;
  }
}

export interface ToneTagPlugin {
  echo(options: { value: string }): Promise<{value: string}>;
}
