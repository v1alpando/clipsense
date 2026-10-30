import { Route, BrowserRouter, Routes } from 'react-router-dom';
import { Layout } from './Layout';
import { HomePage } from '@/pages/HomePage/HomePage';
import { VideoPage } from '@/pages/VideoPage/VideoPage';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="videos/:videoId" element={<VideoPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
