import { WebPlugin } from '@capacitor/core';
import { ToneTagPlugin } from './definitions';

export class ToneTagWeb extends WebPlugin implements ToneTagPlugin {
  constructor() {
    super({
      name: 'ToneTag',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  }
}

const ToneTag = new ToneTagWeb();

export { ToneTag };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(ToneTag);
